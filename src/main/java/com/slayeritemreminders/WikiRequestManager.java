package com.slayeritemreminders;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.callback.ClientThread;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Singleton
final class WikiRequestManager
{
	private static final HttpUrl WIKI_API = HttpUrl.get("https://oldschool.runescape.wiki/api.php");
	private static final String USER_AGENT = "slayer-item-reminders/0.1.0 (RuneLite external plugin)";
	private static final String MAX_LAG_SECONDS = "5";
	private static final long TIMEOUT_SECONDS = 30;
	private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final ClientThread clientThread;

	@Inject
	WikiRequestManager(OkHttpClient httpClient, Gson gson, ClientThread clientThread)
	{
		this.httpClient = httpClient;
		this.gson = gson;
		this.clientThread = clientThread;
	}

	static HttpUrl.Builder apiUrl()
	{
		return WIKI_API.newBuilder();
	}

	RequestHandle request(HttpUrl.Builder urlBuilder, Consumer<JsonObject> success,
		Consumer<Exception> failure)
	{
		Request request = new Request.Builder()
			.url(urlBuilder.addQueryParameter("maxlag", MAX_LAG_SECONDS).build())
			.header("User-Agent", USER_AGENT)
			.build();
		Call call = httpClient.newCall(request);
		call.timeout().timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		RequestHandle handle = new RequestHandle(call);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException exception)
			{
				deliver(handle, () -> failure.accept(exception));
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
					JsonObject result = gson.fromJson(readBounded(body), JsonObject.class);
					deliver(handle, () -> success.accept(result));
				}
				catch (Exception exception)
				{
					deliver(handle, () -> failure.accept(exception));
				}
			}
		});
		return handle;
	}

	private void deliver(RequestHandle handle, Runnable callback)
	{
		if (!handle.cancelled)
		{
			clientThread.invoke(() ->
			{
				if (!handle.cancelled)
				{
					callback.run();
				}
			});
		}
	}

	private static String readBounded(ResponseBody body) throws IOException
	{
		long contentLength = body.contentLength();
		if (contentLength > MAX_RESPONSE_BYTES)
		{
			throw new IOException("OSRS Wiki response exceeded " + MAX_RESPONSE_BYTES + " bytes");
		}
		try (InputStream input = body.byteStream();
			ByteArrayOutputStream output = new ByteArrayOutputStream(
				contentLength > 0 ? (int) contentLength : 8192))
		{
			byte[] buffer = new byte[8192];
			int total = 0;
			int read;
			while ((read = input.read(buffer)) != -1)
			{
				total += read;
				if (total > MAX_RESPONSE_BYTES)
				{
					throw new IOException("OSRS Wiki response exceeded " + MAX_RESPONSE_BYTES + " bytes");
				}
				output.write(buffer, 0, read);
			}
			return new String(output.toByteArray(), StandardCharsets.UTF_8);
		}
	}

	static final class RequestHandle
	{
		private final Call call;
		private volatile boolean cancelled;

		private RequestHandle(Call call)
		{
			this.call = call;
		}

		void cancel()
		{
			cancelled = true;
			call.cancel();
		}
	}
}
