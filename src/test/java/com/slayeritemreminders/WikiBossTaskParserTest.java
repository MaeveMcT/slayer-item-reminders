package com.slayeritemreminders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.*;

public class WikiBossTaskParserTest
{
	@Test
	public void discoversAllExplicitPairsWithoutCollectingUnrelatedLinks() throws IOException
	{
		Map<String, List<TaskVariant>> bosses = WikiBossTaskParser.parse(fixture());
		assertEquals(5, bosses.size());
		assertPair(bosses, "Callisto", "Artio");
		assertPair(bosses, "Venenatis", "Spindel");
		assertPair(bosses, "Vet'ion", "Calvar'ion");
		assertPair(bosses, "Crazy archaeologist", "Deranged archaeologist");
		assertEquals(1, bosses.get("barrows brothers").size());
		assertEquals("Barrows brothers", bosses.get("barrows brothers").get(0).getWikiPage());
		assertFalse(bosses.containsKey("artio")); // Substitutes do not create reverse assignments.
		assertFalse(bosses.containsKey("scurrius"));
		assertEquals("Not for Konar assignments",
			bosses.get("crazy archaeologist").get(1).getEligibilityNote());
		assertNull(bosses.get("callisto").get(1).getEligibilityNote());
	}

	@Test
	public void discoversAnUnseenBossPairWithTheSameSchema()
	{
		Map<String, List<TaskVariant>> bosses = WikiBossTaskParser.parse(
			"== Assignable bosses ==\n{|\n!Boss\n|-\n"
				+ "|{{ilinkt|Future boss|pic=Future pet}}<sub>([[Future substitute|Small boss]] may be killed instead)</sub>\n"
				+ "|[[Future dungeon]]\n|}\n");
		assertPair(bosses, "Future boss", "Future substitute");
		assertEquals("Small boss", bosses.get("future boss").get(1).getName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingTableIsUnavailableRatherThanEvidenceOfNoSubstitutes()
	{
		WikiBossTaskParser.parse("==Overview==\n[[Artio]]");
	}

	@Test
	public void unfamiliarAnnotationsDoNotInventEligibility()
	{
		Map<String, List<TaskVariant>> bosses = WikiBossTaskParser.parse(
			"==Assignable bosses==\n{|\n|-\n"
				+ "|{{ilinkt|Future boss}}<sub>([[Other boss]] sometimes counts)</sub>\n|}\n");
		assertEquals(1, bosses.get("future boss").size());
	}

	static String fixture() throws IOException
	{
		try (InputStream stream = WikiBossTaskParserTest.class.getResourceAsStream("/boss-slayer.wikitext"))
		{
			assertNotNull(stream);
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static void assertPair(Map<String, List<TaskVariant>> bosses, String boss, String substitute)
	{
		List<TaskVariant> variants = bosses.get(WikiBossTaskParser.key(boss));
		assertEquals(2, variants.size());
		assertEquals(boss, variants.get(0).getWikiPage());
		assertEquals(substitute, variants.get(1).getWikiPage());
	}
}
