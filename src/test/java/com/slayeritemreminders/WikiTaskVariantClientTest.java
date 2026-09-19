package com.slayeritemreminders;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import okhttp3.HttpUrl;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WikiTaskVariantClientTest
{
	@Test
	public void fallsBackToMonsterBucketWhenTaskPagesDoNotExist()
	{
		WikiRequestManager manager = mock(WikiRequestManager.class);
		WikiRequestManager.RequestHandle handle = mock(WikiRequestManager.RequestHandle.class);
		List<HttpUrl.Builder> urls = new ArrayList<>();
		List<Consumer<JsonObject>> successes = new ArrayList<>();
		when(manager.request(any(HttpUrl.Builder.class), any(), any())).thenAnswer(invocation ->
		{
			urls.add(invocation.getArgument(0));
			successes.add(invocation.getArgument(1));
			return handle;
		});
		WikiTaskVariantClient client = new WikiTaskVariantClient(manager);
		AtomicReference<TaskVariantDiscoveryResult> result = new AtomicReference<>();

		client.lookup("Mystery", result::set);
		assertEquals("parse", urls.get(0).build().queryParameter("action"));
		successes.get(0).accept(missingPage());
		successes.get(1).accept(missingPage());

		assertEquals("bucket", urls.get(2).build().queryParameter("action"));
		String query = urls.get(2).build().queryParameter("query");
		assertEquals(true, query.contains("{'slayer_category','Mystery'}"));
		assertEquals(true, query.contains("{'slayer_category','Mysteries'}"));
		successes.get(2).accept(json("{\"bucket\":["
			+ "{\"name\":\"Mystery boss\",\"page_name\":\"Mystery boss\"}]}"));

		assertFalse(result.get().isUnavailable());
		assertEquals(1, result.get().getVariants().size());
		assertEquals("Mystery boss", result.get().getVariants().get(0).getWikiPage());
	}

	private static JsonObject missingPage()
	{
		return json("{\"error\":{\"code\":\"missingtitle\"}}");
	}

	private static JsonObject json(String value)
	{
		return new JsonParser().parse(value).getAsJsonObject();
	}
}
