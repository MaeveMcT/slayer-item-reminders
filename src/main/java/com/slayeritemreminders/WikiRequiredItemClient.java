package com.slayeritemreminders;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
final class WikiRequiredItemClient
{
	private static final HttpUrl WIKI_API = HttpUrl.get("https://oldschool.runescape.wiki/api.php");
	private static final String USER_AGENT = "slayer-item-reminders/0.1.0 (RuneLite external plugin)";
	private static final String THROTTLE_KEY = "slayer-required-items";
	private static final String MAX_LAG_SECONDS = "5";
	private static final long TIMEOUT_SECONDS = 30;

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final ClientThread clientThread;
	private final WikiRequestThrottle requestThrottle = new WikiRequestThrottle();
	private final List<Consumer<Map<String, List<ReminderItem>>>> listeners = new ArrayList<>();

	private Map<String, List<ReminderItem>> sessionResult;
	private Call sourceCall;
	private Call itemCall;

	@Inject
	WikiRequiredItemClient(OkHttpClient httpClient, Gson gson, ClientThread clientThread)
	{
		this.httpClient = httpClient;
		this.gson = gson;
		this.clientThread = clientThread;
	}

	void lookup(Consumer<Map<String, List<ReminderItem>>> listener)
	{
		if (sessionResult != null)
		{
			listener.accept(sessionResult);
			return;
		}
		listeners.add(listener);
		if (sourceCall != null || itemCall != null || listeners.size() > 1)
		{
			return;
		}
		if (!requestThrottle.shouldRequest(THROTTLE_KEY, Instant.now()))
		{
			completeListeners(Collections.emptyMap());
			return;
		}
		requestSource();
	}

	void reset()
	{
		if (sourceCall != null)
		{
			sourceCall.cancel();
			sourceCall = null;
		}
		if (itemCall != null)
		{
			itemCall.cancel();
			itemCall = null;
		}
		listeners.clear();
		sessionResult = null;
		requestThrottle.reset();
	}

	private void requestSource()
	{
		HttpUrl url = WIKI_API.newBuilder()
			.addQueryParameter("action", "parse")
			.addQueryParameter("page", "Slayer monsters")
			.addQueryParameter("prop", "wikitext")
			.addQueryParameter("section", "1")
			.addQueryParameter("format", "json")
			.addQueryParameter("maxlag", MAX_LAG_SECONDS)
			.build();
		Request request = new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();
		Call call = httpClient.newCall(request);
		call.timeout().timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		sourceCall = call;
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException exception)
			{
				clientThread.invoke(() -> failSource(call, exception));
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
					String wikiText = root.getAsJsonObject("parse")
						.getAsJsonObject("wikitext").get("*").getAsString();
					List<WikiRequiredItemRule> rules = WikiRequiredItemParser.parse(wikiText);
					clientThread.invoke(() -> requestItems(call, rules));
				}
				catch (Exception exception)
				{
					clientThread.invoke(() -> failSource(call, exception));
				}
			}
		});
	}

	private void requestItems(Call completedSourceCall, List<WikiRequiredItemRule> rules)
	{
		if (sourceCall != completedSourceCall)
		{
			return;
		}
		sourceCall = null;
		if (rules.isEmpty())
		{
			complete(Collections.emptyMap());
			return;
		}

		String query = WikiRequiredItemResolver.buildBucketQuery(rules);
		HttpUrl url = WIKI_API.newBuilder()
			.addQueryParameter("action", "bucket")
			.addQueryParameter("query", query)
			.addQueryParameter("format", "json")
			.addQueryParameter("maxlag", MAX_LAG_SECONDS)
			.build();
		Request request = new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();
		Call call = httpClient.newCall(request);
		call.timeout().timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		itemCall = call;
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException exception)
			{
				clientThread.invoke(() -> failItems(call, exception));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						throw new IOException("OSRS Wiki Bucket returned HTTP " + response.code());
					}
					JsonObject root = gson.fromJson(body.charStream(), JsonObject.class);
					if (root.has("error"))
					{
						throw new IOException("OSRS Wiki Bucket error: " + root.get("error").getAsString());
					}
					Map<String, List<ReminderItem>> result = WikiRequiredItemResolver.resolve(rules, root);
					clientThread.invoke(() -> completeItems(call, result));
				}
				catch (Exception exception)
				{
					clientThread.invoke(() -> failItems(call, exception));
				}
			}
		});
	}

	private void completeItems(Call call, Map<String, List<ReminderItem>> result)
	{
		if (itemCall != call)
		{
			return;
		}
		itemCall = null;
		complete(result);
	}

	private void complete(Map<String, List<ReminderItem>> result)
	{
		requestThrottle.recordSuccess(THROTTLE_KEY);
		sessionResult = result;
		completeListeners(result);
	}

	private void failSource(Call call, Exception exception)
	{
		if (sourceCall != call)
		{
			return;
		}
		sourceCall = null;
		fail(exception);
	}

	private void failItems(Call call, Exception exception)
	{
		if (itemCall != call)
		{
			return;
		}
		itemCall = null;
		fail(exception);
	}

	private void fail(Exception exception)
	{
		requestThrottle.recordFailure(THROTTLE_KEY, Instant.now());
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
