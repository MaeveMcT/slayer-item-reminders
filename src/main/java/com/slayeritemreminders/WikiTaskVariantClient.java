package com.slayeritemreminders;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;

@Slf4j
final class WikiTaskVariantClient
{
	private static final String TASK_PAGE_PREFIX = "Slayer task/";

	private final WikiRequestManager requestManager;
	private final Map<String, List<TaskVariant>> sessionResults = new HashMap<>();
	private final Map<String, List<Consumer<List<TaskVariant>>>> listeners = new HashMap<>();
	private final Map<String, WikiRequestManager.RequestHandle> requests = new HashMap<>();

	@Inject
	WikiTaskVariantClient(WikiRequestManager requestManager)
	{
		this.requestManager = requestManager;
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

	void cancelPendingExcept(String taskName)
	{
		List<String> superseded = new ArrayList<>();
		for (String pendingTask : requests.keySet())
		{
			if (!pendingTask.equals(taskName))
			{
				superseded.add(pendingTask);
			}
		}
		for (String pendingTask : superseded)
		{
			requests.remove(pendingTask).cancel();
			listeners.remove(pendingTask);
		}
	}

	void reset()
	{
		requests.values().forEach(WikiRequestManager.RequestHandle::cancel);
		requests.clear();
		listeners.clear();
		sessionResults.clear();
	}

	private void requestCandidate(String taskName, List<String> candidates, int candidateIndex)
	{
		String candidate = candidates.get(candidateIndex);
		HttpUrl.Builder url = WikiRequestManager.apiUrl()
			.addQueryParameter("action", "parse")
			.addQueryParameter("page", TASK_PAGE_PREFIX + candidate)
			.addQueryParameter("prop", "wikitext")
			.addQueryParameter("redirects", "1")
			.addQueryParameter("format", "json");
		WikiRequestManager.RequestHandle request = requestManager.request(url,
			root -> acceptCandidate(taskName, candidates, candidateIndex, root),
			exception -> fail(taskName, exception));
		requests.put(taskName, request);
	}

	private void acceptCandidate(String taskName, List<String> candidates, int candidateIndex,
		JsonObject root)
	{
		if (!requests.containsKey(taskName))
		{
			return;
		}
		if (isMissingPage(root))
		{
			int nextIndex = candidateIndex + 1;
			if (nextIndex < candidates.size())
			{
				requestCandidate(taskName, candidates, nextIndex);
				return;
			}
			log.debug("No Slayer task Wiki page found for {} using candidates {}", taskName, candidates);
			complete(taskName, Collections.emptyList());
			return;
		}

		try
		{
			String wikiText = root.getAsJsonObject("parse")
				.getAsJsonObject("wikitext").get("*").getAsString();
			complete(taskName, WikiTaskVariantParser.parse(wikiText));
		}
		catch (Exception exception)
		{
			fail(taskName, exception);
		}
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
		String lowerName = taskName.toLowerCase(Locale.ENGLISH);
		if (lowerName.equals("black demon") || lowerName.equals("black demons"))
		{
			candidates.add("Black demons");
		}
		candidates.add(taskName);

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

	private void complete(String taskName, List<TaskVariant> result)
	{
		if (requests.remove(taskName) == null)
		{
			return;
		}
		List<TaskVariant> immutableResult = Collections.unmodifiableList(new ArrayList<>(result));
		sessionResults.put(taskName, immutableResult);
		notifyListeners(taskName, immutableResult);
	}

	private void fail(String taskName, Exception exception)
	{
		if (requests.remove(taskName) == null)
		{
			return;
		}
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
