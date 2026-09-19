package com.slayeritemreminders;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import net.runelite.client.callback.ClientThread;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Timeout;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WikiClientRequestPolicyTest
{
	@Test
	public void dropRequestsDeclareMaxLagAndCancelSupersededLookup()
	{
		OkHttpClient httpClient = mock(OkHttpClient.class);
		Call ankouCall = call();
		Call blackDemonCall = call();
		when(httpClient.newCall(any(Request.class))).thenReturn(ankouCall, blackDemonCall);
		WikiDropTableClient client = new WikiDropTableClient(
			httpClient, mock(Gson.class), mock(ClientThread.class));

		client.lookup("Ankou", ignored -> { });
		client.lookup("Black demon", ignored -> { });
		client.cancelPendingExcept("Black demon");

		ArgumentCaptor<Request> requests = ArgumentCaptor.forClass(Request.class);
		verify(httpClient, org.mockito.Mockito.times(2)).newCall(requests.capture());
		assertEquals("5", requests.getAllValues().get(0).url().queryParameter("maxlag"));
		verify(ankouCall).cancel();
		verify(blackDemonCall, never()).cancel();
	}

	@Test
	public void dropFailureSuppressesImmediateRetry()
	{
		OkHttpClient httpClient = mock(OkHttpClient.class);
		Call failedCall = call();
		when(httpClient.newCall(any(Request.class))).thenReturn(failedCall);
		WikiDropTableClient client = new WikiDropTableClient(
			httpClient, mock(Gson.class), immediateClientThread());
		client.lookup("Ankou", ignored -> { });
		ArgumentCaptor<Callback> callback = ArgumentCaptor.forClass(Callback.class);
		verify(failedCall).enqueue(callback.capture());
		callback.getValue().onFailure(failedCall, new IOException("offline"));
		AtomicBoolean cooledDownResult = new AtomicBoolean();

		client.lookup("Ankou", ignored -> cooledDownResult.set(true));

		verify(httpClient).newCall(any(Request.class));
		assertEquals(true, cooledDownResult.get());
	}

	@Test
	public void variantFailureSuppressesImmediateRetry()
	{
		OkHttpClient httpClient = mock(OkHttpClient.class);
		Call failedCall = call();
		when(httpClient.newCall(any(Request.class))).thenReturn(failedCall);
		WikiTaskVariantClient client = new WikiTaskVariantClient(
			httpClient, mock(Gson.class), immediateClientThread());
		client.lookup("Black demons", ignored -> { });
		ArgumentCaptor<Callback> callback = ArgumentCaptor.forClass(Callback.class);
		verify(failedCall).enqueue(callback.capture());
		callback.getValue().onFailure(failedCall, new IOException("offline"));
		AtomicBoolean cooledDownResult = new AtomicBoolean();

		client.lookup("Black demons", ignored -> cooledDownResult.set(true));

		verify(httpClient).newCall(any(Request.class));
		assertEquals(true, cooledDownResult.get());
	}

	@Test
	public void requiredItemSourceUsesOneCentralSectionRequest()
	{
		OkHttpClient httpClient = mock(OkHttpClient.class);
		Call requiredItemCall = call();
		when(httpClient.newCall(any(Request.class))).thenReturn(requiredItemCall);
		WikiRequiredItemClient client = new WikiRequiredItemClient(
			httpClient, mock(Gson.class), mock(ClientThread.class));

		client.lookup(ignored -> { });

		ArgumentCaptor<Request> request = ArgumentCaptor.forClass(Request.class);
		verify(httpClient).newCall(request.capture());
		assertEquals("Slayer monsters", request.getValue().url().queryParameter("page"));
		assertEquals("1", request.getValue().url().queryParameter("section"));
		assertEquals("5", request.getValue().url().queryParameter("maxlag"));
	}

	@Test
	public void variantRequestsDeclareMaxLag()
	{
		OkHttpClient httpClient = mock(OkHttpClient.class);
		Call variantCall = call();
		when(httpClient.newCall(any(Request.class))).thenReturn(variantCall);
		WikiTaskVariantClient client = new WikiTaskVariantClient(
			httpClient, mock(Gson.class), mock(ClientThread.class));

		client.lookup("Black demons", ignored -> { });

		ArgumentCaptor<Request> request = ArgumentCaptor.forClass(Request.class);
		verify(httpClient).newCall(request.capture());
		assertEquals("5", request.getValue().url().queryParameter("maxlag"));
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
}
