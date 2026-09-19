package com.slayeritemreminders;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;

@Slf4j
final class WikiDropTableClient
{
	private final WikiRequestManager requestManager;
	private final Map<String, Set<RecommendedItem>> sessionResults = new HashMap<>();
	private final Map<String, List<Consumer<Set<RecommendedItem>>>> listeners = new HashMap<>();
	private final Map<String, WikiRequestManager.RequestHandle> requests = new HashMap<>();

	@Inject
	WikiDropTableClient(WikiRequestManager requestManager)
	{
		this.requestManager = requestManager;
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
		HttpUrl.Builder url = WikiRequestManager.apiUrl()
			.addQueryParameter("action", "parse")
			.addQueryParameter("page", wikiPage)
			.addQueryParameter("prop", "text")
			.addQueryParameter("format", "json");
		WikiRequestManager.RequestHandle request = requestManager.request(url,
			root -> complete(wikiPage, root),
			exception -> fail(wikiPage, exception));
		requests.put(wikiPage, request);
	}

	void cancelPendingExcept(String wikiPage)
	{
		Iterator<Map.Entry<String, WikiRequestManager.RequestHandle>> iterator =
			requests.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<String, WikiRequestManager.RequestHandle> entry = iterator.next();
			if (!Objects.equals(entry.getKey(), wikiPage))
			{
				iterator.remove();
				listeners.remove(entry.getKey());
				entry.getValue().cancel();
			}
		}
	}

	void reset()
	{
		requests.values().forEach(WikiRequestManager.RequestHandle::cancel);
		requests.clear();
		listeners.clear();
		sessionResults.clear();
	}

	private void complete(String wikiPage, JsonObject root)
	{
		if (!requests.containsKey(wikiPage))
		{
			return;
		}
		try
		{
			String html = root.getAsJsonObject("parse")
				.getAsJsonObject("text").get("*").getAsString();
			Set<RecommendedItem> parsed = DropTableParser.parse(html);
			Set<RecommendedItem> result = Collections.unmodifiableSet(
				parsed.isEmpty() ? EnumSet.noneOf(RecommendedItem.class) : EnumSet.copyOf(parsed));
			requests.remove(wikiPage);
			sessionResults.put(wikiPage, result);
			notifyListeners(wikiPage, result);
		}
		catch (Exception exception)
		{
			fail(wikiPage, exception);
		}
	}

	private void fail(String wikiPage, Exception exception)
	{
		if (requests.remove(wikiPage) == null)
		{
			return;
		}
		log.debug("Unable to retrieve OSRS Wiki drop table for {}", wikiPage, exception);
		notifyListeners(wikiPage, Collections.emptySet());
	}

	private void notifyListeners(String wikiPage, Set<RecommendedItem> result)
	{
		List<Consumer<Set<RecommendedItem>>> pageListeners = listeners.remove(wikiPage);
		if (pageListeners != null)
		{
			pageListeners.forEach(listener -> listener.accept(result));
		}
	}
}
