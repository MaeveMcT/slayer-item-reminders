package com.slayeritemreminders;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;

@Slf4j
final class WikiRequiredItemClient
{
	private final WikiRequestManager requestManager;
	private final List<Consumer<Map<String, List<ReminderItem>>>> listeners = new ArrayList<>();

	private Map<String, List<ReminderItem>> sessionResult;
	private WikiRequestManager.RequestHandle sourceRequest;
	private WikiRequestManager.RequestHandle itemRequest;

	@Inject
	WikiRequiredItemClient(WikiRequestManager requestManager)
	{
		this.requestManager = requestManager;
	}

	void lookup(Consumer<Map<String, List<ReminderItem>>> listener)
	{
		if (sessionResult != null)
		{
			listener.accept(sessionResult);
			return;
		}
		listeners.add(listener);
		if (sourceRequest != null || itemRequest != null || listeners.size() > 1)
		{
			return;
		}
		requestSource();
	}

	void reset()
	{
		if (sourceRequest != null)
		{
			sourceRequest.cancel();
			sourceRequest = null;
		}
		if (itemRequest != null)
		{
			itemRequest.cancel();
			itemRequest = null;
		}
		listeners.clear();
		sessionResult = null;
	}

	private void requestSource()
	{
		HttpUrl.Builder url = WikiRequestManager.apiUrl()
			.addQueryParameter("action", "parse")
			.addQueryParameter("page", "Slayer monsters")
			.addQueryParameter("prop", "wikitext")
			.addQueryParameter("section", "1")
			.addQueryParameter("format", "json");
		sourceRequest = requestManager.request(url, this::acceptSource, this::failSource);
	}

	private void acceptSource(JsonObject root)
	{
		if (sourceRequest == null)
		{
			return;
		}
		sourceRequest = null;
		try
		{
			String wikiText = root.getAsJsonObject("parse")
				.getAsJsonObject("wikitext").get("*").getAsString();
			List<WikiRequiredItemRule> rules = WikiRequiredItemParser.parse(wikiText);
			if (rules.isEmpty())
			{
				complete(Collections.emptyMap());
				return;
			}
			requestItems(rules);
		}
		catch (Exception exception)
		{
			fail(exception);
		}
	}

	private void requestItems(List<WikiRequiredItemRule> rules)
	{
		String query = WikiRequiredItemResolver.buildBucketQuery(rules);
		HttpUrl.Builder url = WikiRequestManager.apiUrl()
			.addQueryParameter("action", "bucket")
			.addQueryParameter("query", query)
			.addQueryParameter("format", "json");
		itemRequest = requestManager.request(url,
			root -> acceptItems(rules, root), this::failItems);
	}

	private void acceptItems(List<WikiRequiredItemRule> rules, JsonObject root)
	{
		if (itemRequest == null)
		{
			return;
		}
		itemRequest = null;
		try
		{
			if (root.has("error"))
			{
				throw new IOException("OSRS Wiki Bucket error: " + root.get("error").getAsString());
			}
			complete(WikiRequiredItemResolver.resolve(rules, root));
		}
		catch (Exception exception)
		{
			fail(exception);
		}
	}

	private void complete(Map<String, List<ReminderItem>> result)
	{
		sessionResult = result;
		completeListeners(result);
	}

	private void failSource(Exception exception)
	{
		if (sourceRequest == null)
		{
			return;
		}
		sourceRequest = null;
		fail(exception);
	}

	private void failItems(Exception exception)
	{
		if (itemRequest == null)
		{
			return;
		}
		itemRequest = null;
		fail(exception);
	}

	private void fail(Exception exception)
	{
		log.debug("Unable to retrieve centralized Slayer required items", exception);
		completeListeners(Collections.emptyMap());
	}

	private void completeListeners(Map<String, List<ReminderItem>> result)
	{
		List<Consumer<Map<String, List<ReminderItem>>>> pending = new ArrayList<>(listeners);
		listeners.clear();
		pending.forEach(listener -> listener.accept(result));
	}
}
