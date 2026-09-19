package com.slayeritemreminders;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WikiRequiredItemParserTest
{
	@Test
	public void parsesAlternativesAndCarriesRowspanToVariant()
	{
		String wikiText = table(
			"|-\n| rowspan=\"2\" |15\n|{{ilinkt|Banshee}}\n|[[Screaming banshee]]\n|{{NA}}\n|23\n|22\n"
				+ "| rowspan=\"2\" |{{plinkp|Earmuffs}} {{plinkp|Slayer helmet}}\n|drops\n|location\n|masters\n"
				+ "|-\n|{{plinkt|Twisted Banshee|pic=Banshee icon}}\n|[[Screaming twisted banshee]]\n|89\n|100\n|drops\n|location\n");

		List<WikiRequiredItemRule> rules = WikiRequiredItemParser.parse(wikiText);

		assertEquals(2, rules.size());
		assertEquals("Banshee", rules.get(0).getMonster());
		assertEquals("Twisted Banshee", rules.get(1).getMonster());
		assertEquals(2, rules.get(1).getRequirementGroups().get(0).size());
		assertEquals("Earmuffs", rules.get(1).getRequirementGroups().get(0).get(0));
	}

	@Test
	public void preservesCompoundWarpedTerrorbirdRequirements()
	{
		String wikiText = table(
			"|-\n|56\n|{{ilinkt|Warped Terrorbird}}\n|superior\n|attribute\n|96\n|150\n"
				+ "|{{plinkp|Crystal chime}} {{plinkp|Earmuffs}} {{plinkp|Slayer helmet}}\n"
				+ "|drops\n|location\n|masters\n");

		WikiRequiredItemRule rule = WikiRequiredItemParser.parse(wikiText).get(0);

		assertEquals(2, rule.getRequirementGroups().size());
		assertEquals("Crystal chime", rule.getRequirementGroups().get(0).get(0));
		assertEquals(2, rule.getRequirementGroups().get(1).size());
	}

	@Test
	public void ignoresFungicideRefillAsAStandaloneAlternative()
	{
		String wikiText = table(
			"|-\n|57\n|{{ilinkt|Mutated zygomite}}\n|superior\n|attribute\n|74\n|65\n"
				+ "|{{plinkp|Fungicide spray}} {{plinkp|Fungicide}}\n"
				+ "|drops\n|location\n|masters\n");

		WikiRequiredItemRule rule = WikiRequiredItemParser.parse(wikiText).get(0);

		assertEquals(1, rule.getRequirementGroups().get(0).size());
		assertEquals("Fungicide spray", rule.getRequirementGroups().get(0).get(0));
	}

	private static String table(String rows)
	{
		return "{| class=\"wikitable\"\n!Slayer\n!Monster\n!Superior\n!Attributes\n"
			+ "!Combat\n!XP\n!Required\n!Drops\n!Location\n!Masters\n" + rows + "|}";
	}
}
