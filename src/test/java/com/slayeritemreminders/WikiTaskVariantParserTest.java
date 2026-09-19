package com.slayeritemreminders;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WikiTaskVariantParserTest
{
	@Test
	public void parsesAndDeduplicatesMonsterVariantRows()
	{
		String wikiText = "Introduction\n"
			+ "==Monster variants==\n"
			+ "{| class=\"wikitable\"\n!Monster\n!Level\n"
			+ "|-\n|[[Greater Demon]]\n|92\n"
			+ "|-\n|[[Greater Demon]]\n|100\n"
			+ "|-\n|[[Tormented Demon|Tormented demon]]\n|450\n"
			+ "|}\n==Locations==\n|[[Not a monster]]\n";

		List<TaskVariant> variants = WikiTaskVariantParser.parse(wikiText);

		assertEquals(2, variants.size());
		assertEquals("Greater Demon", variants.get(0).getWikiPage());
		assertEquals("Tormented demon", variants.get(1).getName());
		assertEquals("Tormented Demon", variants.get(1).getWikiPage());
	}

	@Test
	public void parsesEveryMonsterLinkedInOneCell()
	{
		String wikiText = "==Monster variants==\n"
			+ "{| class=\"wikitable\"\n!Monster\n!Location\n"
			+ "|-\n|[[Dagannoth Prime]]\n[[Dagannoth Rex]]\n\n[[Dagannoth Supreme]]\n"
			+ "|[[Waterbirth Island Dungeon]]\n|}\n";

		List<TaskVariant> variants = WikiTaskVariantParser.parse(wikiText);

		assertEquals(3, variants.size());
		assertEquals("Dagannoth Prime", variants.get(0).getWikiPage());
		assertEquals("Dagannoth Rex", variants.get(1).getWikiPage());
		assertEquals("Dagannoth Supreme", variants.get(2).getWikiPage());
	}

	@Test
	public void parsesCaveKrakenBossVariant()
	{
		String wikiText = "==Monster Variants==\n"
			+ "{| class=\"wikitable\"\n!Monster\n!Location\n"
			+ "|-\n|[[Cave kraken]]\n|[[Kraken Cove]]\n"
			+ "|-\n|[[Kraken]]\n|[[Kraken Cove]]\n|}\n";

		List<TaskVariant> variants = WikiTaskVariantParser.parse(wikiText);

		assertEquals(2, variants.size());
		assertEquals("Kraken", variants.get(1).getWikiPage());
	}

	@Test
	public void generatesPluralFallbackForCaveKrakenTaskPage()
	{
		assertEquals("Cave kraken", WikiTaskVariantClient.taskPageCandidates("Cave kraken").get(0));
		assertEquals("Cave krakens", WikiTaskVariantClient.taskPageCandidates("Cave kraken").get(1));
	}

	@Test
	public void generatesSingularFallbackForPluralTaskPage()
	{
		assertEquals("Greater demon", WikiTaskVariantClient.taskPageCandidates("Greater demons").get(1));
	}

	@Test
	public void usesCanonicalBlackDemonsTaskPageForEitherTaskName()
	{
		assertEquals("Black demons", WikiTaskVariantClient.taskPageCandidates("Black demons").get(0));
		assertEquals("Black demons", WikiTaskVariantClient.taskPageCandidates("Black demon").get(0));
	}

	@Test
	public void returnsEmptyListWithoutVariantSection()
	{
		assertTrue(WikiTaskVariantParser.parse("==Locations==\nNothing here").isEmpty());
	}
}
