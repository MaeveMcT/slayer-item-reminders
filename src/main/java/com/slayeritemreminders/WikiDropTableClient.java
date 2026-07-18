package com.slayeritemreminders;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
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
final class WikiDropTableClient
{
	private static final HttpUrl WIKI_API = HttpUrl.get("https://oldschool.runescape.wiki/api.php");
	private static final String USER_AGENT = "slayer-item-reminders/0.1.0 (RuneLite external plugin)";
	private static final long TIMEOUT_SECONDS = 30;

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final ClientThread clientThread;
	private final Map<String, Set<RecommendedItem>> sessionResults = new HashMap<>();
	private final Map<String, List<Consumer<Set<RecommendedItem>>>> listeners = new HashMap<>();
	private final Map<String, Call> calls = new HashMap<>();

	@Inject
	WikiDropTableClient(OkHttpClient httpClient, Gson gson, ClientThread clientThread)
	{
		this.httpClient = httpClient;
		this.gson = gson;
		this.clientThread = clientThread;
	}

	void lookup(String wikiPage, Consumer<Set<RecommendedItem>> listener)
	{
		Set<RecommendedItem> cached = sessionResults.get(wikiPage);
		if (cached != null)
		{
			listener.accept(cached);
			return;
		}

		List<Consumer<Set<RecommendedItem>>> pageListeners = listeners.get(wikiPage);
		if (pageListeners != null)
		{
			pageListeners.add(listener);
			return;
		}

		pageListeners = new ArrayList<>();
		pageListeners.add(listener);
		listeners.put(wikiPage, pageListeners);

		HttpUrl url = WIKI_API.newBuilder()
			.addQueryParameter("action", "parse")
			.addQueryParameter("page", wikiPage)
			.addQueryParameter("prop", "text")
			.addQueryParameter("format", "json")
			.build();
		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();
		Call call = httpClient.newCall(request);
		call.timeout().timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		calls.put(wikiPage, call);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException exception)
			{
				clientThread.invoke(() -> fail(wikiPage, exception));
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
					String html = root.getAsJsonObject("parse")
						.getAsJsonObject("text")
						.get("*")
						.getAsString();
					Set<RecommendedItem> parsed = DropTableParser.parse(html);
					Set<RecommendedItem> result = Collections.unmodifiableSet(
						parsed.isEmpty() ? EnumSet.noneOf(RecommendedItem.class) : EnumSet.copyOf(parsed));
					clientThread.invoke(() -> complete(wikiPage, result));
				}
				catch (Exception exception)
				{
					clientThread.invoke(() -> fail(wikiPage, exception));
				}
			}
		});
	}

	void reset()
	{
		calls.values().forEach(Call::cancel);
		calls.clear();
		listeners.clear();
		sessionResults.clear();
	}

	private void complete(String wikiPage, Set<RecommendedItem> result)
	{
		calls.remove(wikiPage);
		sessionResults.put(wikiPage, result);
		List<Consumer<Set<RecommendedItem>>> pageListeners = listeners.remove(wikiPage);
		if (pageListeners != null)
		{
			pageListeners.forEach(listener -> listener.accept(result));
		}
	}

	private void fail(String wikiPage, Exception exception)
	{
		calls.remove(wikiPage);
		listeners.remove(wikiPage);
		log.debug("Unable to retrieve OSRS Wiki drop table for {}", wikiPage, exception);
	}
}
