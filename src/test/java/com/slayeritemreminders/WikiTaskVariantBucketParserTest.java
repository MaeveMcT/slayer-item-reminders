package com.slayeritemreminders;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WikiTaskVariantBucketParserTest
{
	@Test
	public void parsesDeduplicatesAndExcludesAlternateGameModes()
	{
		String json = "{\"bucket\":["
			+ "{\"name\":\"Blue dragon\",\"page_name\":\"Blue dragon\"},"
			+ "{\"name\":\"Blue dragon\",\"page_name\":\"Blue dragon\"},"
			+ "{\"name\":\"Baby blue dragon\",\"page_name\":\"Baby blue dragon\"},"
			+ "{\"name\":\"Vorkath\",\"page_name\":\"Vorkath\"},"
			+ "{\"name\":\"Vorkath\",\"page_name\":\"Vorkath (PvM Arena)\"},"
			+ "{\"name\":\"Vorkath (Echo)\",\"page_name\":\"Vorkath (Echo)\"}]}";

		List<TaskVariant> variants = WikiTaskVariantBucketParser.parse(
			new JsonParser().parse(json).getAsJsonObject());

		assertEquals(3, variants.size());
		assertEquals("Blue dragon", variants.get(0).getWikiPage());
		assertEquals("Baby blue dragon", variants.get(1).getWikiPage());
		assertEquals("Vorkath", variants.get(2).getWikiPage());
	}
}
