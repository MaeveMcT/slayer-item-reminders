package com.slayeritemreminders;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import okhttp3.HttpUrl;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WikiTaskVariantClientTest
{
	@Test
	public void discoversDirectBossSubstitutesBeforeTaskPageFallback() throws IOException
	{
		Harness harness = new Harness();
		AtomicReference<TaskVariantDiscoveryResult> result = new AtomicReference<>();
		harness.client.lookup("Callisto", result::set);
		assertEquals("Boss Slayer", harness.calls.get(0).url.queryParameter("page"));
		harness.bosses();
		assertFalse(result.get().isUnavailable());
		assertEquals(2, result.get().getVariants().size());
		assertEquals("Artio", result.get().getVariants().get(1).getWikiPage());
		assertEquals(1, harness.calls.size());
	}

	@Test
	public void sharesBossTableAcrossCurrentStoredAndLaterLookups() throws IOException
	{
		Harness harness = new Harness();
		AtomicReference<TaskVariantDiscoveryResult> current = new AtomicReference<>();
		AtomicReference<TaskVariantDiscoveryResult> stored = new AtomicReference<>();
		harness.client.lookup("Callisto", current::set);
		harness.client.lookup("Vet'ion", stored::set);
		harness.client.lookup("Callisto", result -> assertEquals(2, result.getVariants().size()));
		assertEquals(1, harness.calls.size());
		harness.bosses();
		assertEquals("Artio", current.get().getVariants().get(1).getWikiPage());
		assertEquals("Calvar'ion", stored.get().getVariants().get(1).getWikiPage());
		harness.client.lookup("Venenatis", result ->
			assertEquals("Spindel", result.getVariants().get(1).getWikiPage()));
		harness.client.lookup("Callisto", result -> assertSame(current.get(), result));
		assertEquals(1, harness.calls.size());
	}

	@Test
	public void joinsGameTaskAliasesToBossRows() throws IOException
	{
		Harness harness = new Harness();
		AtomicReference<TaskVariantDiscoveryResult> result = new AtomicReference<>();
		harness.client.lookup("Crazy Archaeologists", result::set);
		harness.bosses();
		TaskVariant substitute = result.get().getVariants().get(1);
		assertEquals("Deranged archaeologist", substitute.getWikiPage());
		assertEquals("Not for Konar assignments", substitute.getEligibilityNote());
		assertEquals(1, harness.calls.size());
	}

	@Test
	public void fallsBackToMonsterBucketWhenTaskPagesDoNotExist() throws IOException
	{
		Harness harness = new Harness();
		AtomicReference<TaskVariantDiscoveryResult> result = new AtomicReference<>();
		harness.client.lookup("Mystery", result::set);
		harness.bosses();
		assertEquals("Slayer task/Mystery", harness.calls.get(1).url.queryParameter("page"));
		harness.calls.get(1).success.accept(missingPage());
		harness.calls.get(2).success.accept(missingPage());
		assertEquals("bucket", harness.calls.get(3).url.queryParameter("action"));
		String query = harness.calls.get(3).url.queryParameter("query");
		assertTrue(query.contains("{'slayer_category','Mystery'}"));
		assertTrue(query.contains("{'slayer_category','Mysteries'}"));
		harness.calls.get(3).success.accept(json("{\"bucket\":["
			+ "{\"name\":\"Mystery boss\",\"page_name\":\"Mystery boss\"}]}"));
		assertFalse(result.get().isUnavailable());
		assertEquals("Mystery boss", result.get().getVariants().get(0).getWikiPage());
	}

	@Test
	public void preservesRegularTaskPageVariantsAndBossGroupsWithoutExplicitSubstitutes() throws IOException
	{
		Harness harness = new Harness();
		AtomicReference<TaskVariantDiscoveryResult> result = new AtomicReference<>();
		harness.client.lookup("Barrows brothers", result::set);
		harness.bosses();
		assertEquals("Slayer task/Barrows brothers", harness.calls.get(1).url.queryParameter("page"));
		harness.calls.get(1).success.accept(page("==Monster variants==\n{|\n|-\n"
			+ "|[[Ahrim]]\n|Other column\n|-\n|[[Dharok]]\n|Other column\n|}\n"));
		assertEquals(2, result.get().getVariants().size());
		assertEquals("Ahrim", result.get().getVariants().get(0).getWikiPage());
	}

	@Test
	public void preservesBothCurrentAndStoredWaitersWhenCancelling() throws IOException
	{
		Harness harness = new Harness();
		AtomicReference<TaskVariantDiscoveryResult> obsolete = new AtomicReference<>();
		harness.client.lookup("Callisto", result -> { });
		harness.client.lookup("Vet'ion", result -> { });
		harness.client.lookup("Venenatis", obsolete::set);
		harness.client.cancelPendingExcept("Callisto", "Vet'ion");
		verifyNoInteractions(harness.calls.get(0).handle);
		harness.bosses();
		assertNull(obsolete.get());
		assertEquals(1, harness.calls.size());
	}

	@Test
	public void preservesBothCurrentAndStoredRequestsWhenCancellingSupersededTasks() throws IOException
	{
		Harness harness = new Harness();
		harness.client.lookup("Gargoyles", result -> { });
		harness.bosses();
		harness.client.lookup("Blue dragons", result -> { });
		harness.client.lookup("Bears", result -> { });
		harness.client.cancelPendingExcept("Gargoyles", "Blue dragons");
		verify(harness.calls.get(3).handle).cancel();
		verifyNoInteractions(harness.calls.get(1).handle, harness.calls.get(2).handle);
		harness.client.reset();
		verify(harness.calls.get(1).handle).cancel();
		verify(harness.calls.get(2).handle).cancel();
	}

	@Test
	public void resetCancelsSharedFetchAndRejectsItsStaleCallback() throws IOException
	{
		Harness harness = new Harness();
		AtomicReference<TaskVariantDiscoveryResult> obsolete = new AtomicReference<>();
		AtomicReference<TaskVariantDiscoveryResult> fresh = new AtomicReference<>();
		harness.client.lookup("Callisto", obsolete::set);
		harness.client.reset();
		verify(harness.calls.get(0).handle).cancel();
		harness.client.lookup("Vet'ion", fresh::set);
		harness.calls.get(0).success.accept(page(WikiBossTaskParserTest.fixture()));
		assertNull(obsolete.get());
		assertNull(fresh.get());
		harness.calls.get(1).success.accept(page(WikiBossTaskParserTest.fixture()));
		assertEquals("Calvar'ion", fresh.get().getVariants().get(1).getWikiPage());
	}

	@Test
	public void cancellingAllWaitersCancelsSharedFetchAndAllowsRetry() throws IOException
	{
		Harness harness = new Harness();
		AtomicReference<TaskVariantDiscoveryResult> obsolete = new AtomicReference<>();
		harness.client.lookup("Callisto", obsolete::set);
		harness.client.cancelPendingExcept("Gargoyles");
		verify(harness.calls.get(0).handle).cancel();
		harness.client.lookup("Venenatis", result -> { });
		harness.calls.get(0).success.accept(page(WikiBossTaskParserTest.fixture()));
		assertNull(obsolete.get());
		assertEquals(2, harness.calls.size());
	}

	@Test
	public void bossSourceFailureKeepsUsefulVariantsButDoesNotCacheIncompleteResults() throws IOException
	{
		Harness harness = new Harness();
		AtomicReference<TaskVariantDiscoveryResult> result = new AtomicReference<>();
		harness.client.lookup("Callisto", result::set);
		harness.calls.get(0).failure.accept(new IOException("offline"));
		harness.calls.get(1).success.accept(page("==Monster variants==\n{|\n|-\n|[[Callisto]]\n|}\n"));
		assertTrue(result.get().isUnavailable());
		assertEquals("Callisto", result.get().getVariants().get(0).getWikiPage());
		harness.client.lookup("Callisto", result::set);
		assertEquals("Boss Slayer", harness.calls.get(2).url.queryParameter("page"));
		harness.calls.get(2).success.accept(page(WikiBossTaskParserTest.fixture()));
		assertFalse(result.get().isUnavailable());
		assertEquals("Artio", result.get().getVariants().get(1).getWikiPage());
	}

	@Test
	public void successfulBossRetryCannotBeMaskedByAnOlderTaskPageResult() throws IOException
	{
		Harness harness = new Harness();
		AtomicReference<TaskVariantDiscoveryResult> callisto = new AtomicReference<>();
		harness.client.lookup("Callisto", callisto::set);
		harness.calls.get(0).failure.accept(new IOException("offline"));
		harness.client.lookup("Vet'ion", result -> { });
		harness.calls.get(2).success.accept(page(WikiBossTaskParserTest.fixture()));
		harness.calls.get(1).success.accept(page("==Monster variants==\n{|\n|-\n|[[Callisto]]\n|}\n"));
		assertEquals("Artio", callisto.get().getVariants().get(1).getWikiPage());
		harness.client.lookup("Callisto", result -> assertSame(callisto.get(), result));
		assertEquals(3, harness.calls.size());
	}

	@Test
	public void malformedBossSourceAlsoFallsBackWithoutCaching() throws IOException
	{
		Harness harness = new Harness();
		AtomicReference<TaskVariantDiscoveryResult> result = new AtomicReference<>();
		harness.client.lookup("Callisto", result::set);
		harness.calls.get(0).success.accept(page("==Renamed section==\nUnknown layout"));
		harness.calls.get(1).success.accept(page("==Monster variants==\n{|\n|-\n|[[Callisto]]\n|}\n"));
		assertTrue(result.get().isUnavailable());
		harness.client.lookup("Callisto", result::set);
		assertEquals("Boss Slayer", harness.calls.get(2).url.queryParameter("page"));
	}

	private static JsonObject missingPage()
	{
		return json("{\"error\":{\"code\":\"missingtitle\"}}");
	}

	private static JsonObject json(String value)
	{
		return new JsonParser().parse(value).getAsJsonObject();
	}

	private static JsonObject page(String text)
	{
		JsonObject wikiText = new JsonObject();
		wikiText.addProperty("*", text);
		JsonObject parse = new JsonObject();
		parse.add("wikitext", wikiText);
		JsonObject root = new JsonObject();
		root.add("parse", parse);
		return root;
	}

	private static class Harness
	{
		private final List<Request> calls = new ArrayList<>();
		private final WikiTaskVariantClient client;

		private Harness()
		{
			WikiRequestManager manager = mock(WikiRequestManager.class);
			when(manager.request(any(HttpUrl.Builder.class), any(), any())).thenAnswer(invocation ->
			{
				Request call = new Request(invocation.<HttpUrl.Builder>getArgument(0).build(),
					invocation.getArgument(1), invocation.getArgument(2));
				calls.add(call);
				return call.handle;
			});
			client = new WikiTaskVariantClient(manager);
		}

		private void bosses() throws IOException
		{
			calls.get(0).success.accept(page(WikiBossTaskParserTest.fixture()));
		}
	}

	private static class Request
	{
		private final HttpUrl url;
		private final Consumer<JsonObject> success;
		private final Consumer<Exception> failure;
		private final WikiRequestManager.RequestHandle handle = mock(WikiRequestManager.RequestHandle.class);

		private Request(HttpUrl url, Consumer<JsonObject> success, Consumer<Exception> failure)
		{
			this.url = url;
			this.success = success;
			this.failure = failure;
		}
	}
}
