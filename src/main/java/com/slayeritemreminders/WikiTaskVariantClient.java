package com.slayeritemreminders;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
final class WikiTaskVariantClient
{
	private static final HttpUrl WIKI_API = HttpUrl.get("https://oldschool.runescape.wiki/api.php");
	private static final String USER_AGENT = "slayer-item-reminders/0.1.0 (RuneLite external plugin)";
	private static final long TIMEOUT_SECONDS = 30;
	private static final String TASK_PAGE_PREFIX = "Slayer task/";

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final ClientThread clientThread;
	private final Map<String, List<TaskVariant>> sessionResults = new HashMap<>();
	private final Map<String, List<Consumer<List<TaskVariant>>>> listeners = new HashMap<>();
	private final Map<String, Call> calls = new HashMap<>();

	@Inject
	WikiTaskVariantClient(OkHttpClient httpClient, Gson gson, ClientThread clientThread)
	{
		this.httpClient = httpClient;
		this.gson = gson;
		this.clientThread = clientThread;
	}

	void lookup(String taskName, Consumer<List<TaskVariant>> listener)
	{
		List<TaskVariant> cached = sessionResults.get(taskName);
		if (cached != null)
		{
			listener.accept(cached);
			return;
		}

		List<Consumer<List<TaskVariant>>> pageListeners = listeners.get(taskName);
		if (pageListeners != null)
		{
			pageListeners.add(listener);
			return;
		}

		pageListeners = new ArrayList<>();
		pageListeners.add(listener);
		listeners.put(taskName, pageListeners);
		requestCandidate(taskName, taskPageCandidates(taskName), 0);
	}

	private void requestCandidate(String taskName, List<String> candidates, int candidateIndex)
	{
		String candidate = candidates.get(candidateIndex);
		HttpUrl url = WIKI_API.newBuilder()
			.addQueryParameter("action", "parse")
			.addQueryParameter("page", TASK_PAGE_PREFIX + candidate)
			.addQueryParameter("prop", "wikitext")
			.addQueryParameter("redirects", "1")
			.addQueryParameter("format", "json")
			.build();
		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();
		Call call = httpClient.newCall(request);
		call.timeout().timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		calls.put(taskName, call);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException exception)
			{
				clientThread.invoke(() -> fail(taskName, exception));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						throw new IOException("OSRS Wiki returned HTTP " + response.code());
					}

					JsonObject root = gson.fromJson(body.charStream(), JsonObject.class);
					if (isMissingPage(root))
					{
						clientThread.invoke(() -> tryNextCandidate(taskName, candidates, candidateIndex));
						return;
					}

					String wikiText = root.getAsJsonObject("parse")
						.getAsJsonObject("wikitext")
						.get("*")
						.getAsString();
					List<TaskVariant> result = WikiTaskVariantParser.parse(wikiText);
					clientThread.invoke(() -> complete(taskName, result));
				}
				catch (Exception exception)
				{
					clientThread.invoke(() -> fail(taskName, exception));
				}
			}
		});
	}

	private void tryNextCandidate(String taskName, List<String> candidates, int candidateIndex)
	{
		if (!listeners.containsKey(taskName))
		{
			return;
		}

		int nextIndex = candidateIndex + 1;
		if (nextIndex < candidates.size())
		{
			requestCandidate(taskName, candidates, nextIndex);
			return;
		}

		log.debug("No Slayer task Wiki page found for {} using candidates {}", taskName, candidates);
		complete(taskName, Collections.emptyList());
	}

	private static boolean isMissingPage(JsonObject root)
	{
		if (!root.has("error") || !root.get("error").isJsonObject())
		{
			return false;
		}
		JsonObject error = root.getAsJsonObject("error");
		String code = error.has("code") ? error.get("code").getAsString() : "";
		return "missingtitle".equals(code) || "invalidtitle".equals(code);
	}

	static List<String> taskPageCandidates(String taskName)
	{
		Set<String> candidates = new LinkedHashSet<>();
		candidates.add(taskName);

		String lowerName = taskName.toLowerCase(Locale.ENGLISH);
		if (lowerName.endsWith("ies") && taskName.length() > 3)
		{
			candidates.add(taskName.substring(0, taskName.length() - 3) + "y");
		}
		else if (lowerName.endsWith("s") && taskName.length() > 1)
		{
			candidates.add(taskName.substring(0, taskName.length() - 1));
			if (lowerName.endsWith("es") && taskName.length() > 2)
			{
				candidates.add(taskName.substring(0, taskName.length() - 2));
			}
		}
		else if (lowerName.endsWith("y") && taskName.length() > 1
			&& !isVowel(lowerName.charAt(lowerName.length() - 2)))
		{
			candidates.add(taskName.substring(0, taskName.length() - 1) + "ies");
		}
		else
		{
			candidates.add(taskName + "s");
			if (lowerName.endsWith("ch") || lowerName.endsWith("sh")
				|| lowerName.endsWith("x") || lowerName.endsWith("z"))
			{
				candidates.add(taskName + "es");
			}
		}
		return Collections.unmodifiableList(new ArrayList<>(candidates));
	}

	private static boolean isVowel(char character)
	{
		return character == 'a' || character == 'e' || character == 'i'
			|| character == 'o' || character == 'u';
	}

	void reset()
	{
		calls.values().forEach(Call::cancel);
		calls.clear();
		listeners.clear();
		sessionResults.clear();
	}

	private void complete(String taskName, List<TaskVariant> result)
	{
		calls.remove(taskName);
		List<TaskVariant> immutableResult = Collections.unmodifiableList(new ArrayList<>(result));
		sessionResults.put(taskName, immutableResult);
		notifyListeners(taskName, immutableResult);
	}

	private void fail(String taskName, Exception exception)
	{
		calls.remove(taskName);
		log.debug("Unable to retrieve Wiki variants for {}", taskName, exception);
		notifyListeners(taskName, Collections.emptyList());
	}

	private void notifyListeners(String taskName, List<TaskVariant> result)
	{
		List<Consumer<List<TaskVariant>>> pageListeners = listeners.remove(taskName);
		if (pageListeners != null)
		{
			pageListeners.forEach(listener -> listener.accept(result));
		}
	}
}
