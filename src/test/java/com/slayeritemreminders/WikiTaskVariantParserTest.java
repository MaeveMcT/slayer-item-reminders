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
	public void returnsEmptyListWithoutVariantSection()
	{
		assertTrue(WikiTaskVariantParser.parse("==Locations==\nNothing here").isEmpty());
	}
}
