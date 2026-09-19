package com.slayeritemreminders;

import com.google.gson.Gson;
import net.runelite.client.callback.ClientThread;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Timeout;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WikiClientRequestPolicyTest
{
	@Test
	public void managerAppliesSharedPolicy()
	{
		OkHttpClient httpClient = mock(OkHttpClient.class);
		Call call = call();
		when(httpClient.newCall(any(Request.class))).thenReturn(call);
		WikiRequestManager manager = new WikiRequestManager(
			httpClient, new Gson(), immediateClientThread());

		manager.request(url("Ankou"), ignored -> { }, ignored -> { });

		ArgumentCaptor<Request> request = ArgumentCaptor.forClass(Request.class);
		verify(httpClient).newCall(request.capture());
		assertEquals("5", request.getValue().url().queryParameter("maxlag"));
		assertEquals("slayer-item-reminders/0.1.0 (RuneLite external plugin)",
			request.getValue().header("User-Agent"));
		verify(call).timeout();
		verify(call).enqueue(any(Callback.class));
	}

	@Test
	public void managerRejectsOversizedResponses() throws Exception
	{
		OkHttpClient httpClient = mock(OkHttpClient.class);
		Call call = call();
		when(httpClient.newCall(any(Request.class))).thenReturn(call);
		WikiRequestManager manager = new WikiRequestManager(
			httpClient, new Gson(), immediateClientThread());
		boolean[] failed = {false};
		manager.request(url("Large"), ignored -> { }, ignored -> failed[0] = true);
		ArgumentCaptor<Callback> callback = ArgumentCaptor.forClass(Callback.class);
		verify(call).enqueue(callback.capture());
		byte[] oversized = new byte[2 * 1024 * 1024 + 1];

		callback.getValue().onResponse(call, response(oversized));

		assertEquals(true, failed[0]);
	}

	@Test
	public void domainClientsUseSharedManagerAndCancellation()
	{
		WikiRequestManager manager = mock(WikiRequestManager.class);
		WikiRequestManager.RequestHandle handle = mock(WikiRequestManager.RequestHandle.class);
		when(manager.request(any(HttpUrl.Builder.class), any(), any())).thenReturn(handle);
		WikiDropTableClient dropClient = new WikiDropTableClient(manager);

		dropClient.lookup("Ankou", ignored -> { });
		dropClient.cancelPendingExcept("Black demon");

		ArgumentCaptor<HttpUrl.Builder> url = ArgumentCaptor.forClass(HttpUrl.Builder.class);
		verify(manager).request(url.capture(), any(), any());
		assertEquals("Ankou", url.getValue().build().queryParameter("page"));
		verify(handle).cancel();
	}

	private static HttpUrl.Builder url(String page)
	{
		return WikiRequestManager.apiUrl()
			.addQueryParameter("action", "parse")
			.addQueryParameter("page", page)
			.addQueryParameter("format", "json");
	}

	private static ClientThread immediateClientThread()
	{
		ClientThread clientThread = mock(ClientThread.class);
		doAnswer(invocation ->
		{
			invocation.<Runnable>getArgument(0).run();
			return null;
		}).when(clientThread).invoke(any(Runnable.class));
		return clientThread;
	}

	private static Call call()
	{
		Call call = mock(Call.class);
		when(call.timeout()).thenReturn(new Timeout());
		return call;
	}

	private static Response response(byte[] body)
	{
		return new Response.Builder()
			.request(new Request.Builder().url("https://oldschool.runescape.wiki/api.php").build())
			.protocol(Protocol.HTTP_1_1)
			.code(200)
			.message("OK")
			.body(ResponseBody.create(MediaType.get("application/json"), body))
			.build();
	}
}
