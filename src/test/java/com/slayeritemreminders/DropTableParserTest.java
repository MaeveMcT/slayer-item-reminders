package com.slayeritemreminders;

import java.io.IOException;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DropTableParserTest
{
	@Test
	public void recommendsHerbSackForFourDistinctEligibleHerbs() throws IOException
	{
		String html = drops(
			row("Grimy guam leaf", "1"),
			row("Grimy marrentill", "1"),
			row("Grimy tarromin", "1"),
			row("Grimy harralander", "1"),
			row("Grimy harralander", "1"));

		Set<RecommendedItem> recommendations = DropTableParser.parse(html);

		assertTrue(recommendations.contains(RecommendedItem.HERB_SACK));
		assertFalse(recommendations.contains(RecommendedItem.SEED_BOX));
	}

	@Test
	public void recommendsHerbSackForQualifiedDropsHeading() throws IOException
	{
		String html = "<h2 id=\"Members'_worlds_drops\">Members' worlds drops</h2><table>"
			+ row("Grimy guam leaf", "1")
			+ row("Grimy marrentill", "1")
			+ row("Grimy tarromin", "1")
			+ row("Grimy harralander", "1")
			+ "</table><h2 id=\"Trivia\">Trivia</h2>";

		Set<RecommendedItem> recommendations = DropTableParser.parse(html);

		assertTrue(recommendations.contains(RecommendedItem.HERB_SACK));
	}

	@Test
	public void ignoresNotedAndIneligibleItems() throws IOException
	{
		String html = drops(
			row("Grimy guam leaf", "1"),
			row("Grimy marrentill", "1"),
			row("Grimy tarromin", "1"),
			row("Grimy ranarr weed", "5 (noted)"),
			row("Clean ranarr weed", "1"));

		Set<RecommendedItem> recommendations = DropTableParser.parse(html);

		assertFalse(recommendations.contains(RecommendedItem.HERB_SACK));
	}

	@Test
	public void recommendsSeedBoxForFourDistinctEligibleSeeds() throws IOException
	{
		String html = drops(
			row("Cactus seed", "1"),
			row("Potato cactus seed", "1"),
			row("Belladonna seed", "1"),
			row("Mushroom spore", "1"));

		Set<RecommendedItem> recommendations = DropTableParser.parse(html);

		assertTrue(recommendations.contains(RecommendedItem.SEED_BOX));
	}

	@Test
	public void doesNotTreatCrystalSeedsAsSeedBoxSeeds() throws IOException
	{
		String html = drops(
			row("Potato seed", "1"),
			row("Onion seed", "1"),
			row("Cabbage seed", "1"),
			row("Enhanced crystal weapon seed", "1"));

		Set<RecommendedItem> recommendations = DropTableParser.parse(html);

		assertFalse(recommendations.contains(RecommendedItem.SEED_BOX));
	}

	private static String drops(String... rows)
	{
		return "<h2 id=\"Other\">Other</h2><table><tr><td><a title=\"Ranarr seed\">"
			+ "Ranarr seed</a></td></tr></table><h2 id=\"Drops\">Drops</h2><table>"
			+ String.join("", rows) + "</table><h2 id=\"Trivia\">Trivia</h2>";
	}

	private static String row(String item, String quantity)
	{
		return "<tr><td><a title=\"" + item + "\">" + item + "</a></td><td>" + quantity + "</td></tr>";
	}
}
