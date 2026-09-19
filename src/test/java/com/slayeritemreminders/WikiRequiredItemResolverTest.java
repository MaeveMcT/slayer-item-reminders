package com.slayeritemreminders;

import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WikiRequiredItemResolverTest
{
	@Test
	public void buildsOneEscapedQueryForDistinctItemNames()
	{
		List<WikiRequiredItemRule> rules = Arrays.asList(
			new WikiRequiredItemRule("Basilisk", Collections.singletonList(
				Arrays.asList("Mirror shield", "V's shield"))),
			new WikiRequiredItemRule("Cockatrice", Collections.singletonList(
				Collections.singletonList("Mirror shield"))));

		String query = WikiRequiredItemResolver.buildBucketQuery(rules);

		assertEquals(1, occurrences(query, "Mirror shield"));
		assertTrue(query.contains("V\\'s shield"));
		assertTrue(query.contains("limit(500)"));
	}

	@Test
	public void resolvesAlternativeItemsIntoOneReminder()
	{
		WikiRequiredItemRule rule = new WikiRequiredItemRule("Gargoyle",
			Collections.singletonList(Arrays.asList("Rock hammer", "Rock thrownhammer")));
		String json = "{\"bucket\":["
			+ "{\"item_name\":\"Rock hammer\",\"item_id\":[\"4162\"]},"
			+ "{\"item_name\":\"Rock thrownhammer\",\"item_id\":[\"21754\"]}]}";

		Map<String, List<ReminderItem>> result = WikiRequiredItemResolver.resolve(
			Collections.singletonList(rule), new JsonParser().parse(json).getAsJsonObject());

		ReminderItem reminder = result.get("gargoyle").get(0);
		assertEquals("Rock hammer or Rock thrownhammer", reminder.getName());
		assertTrue(contains(reminder.getItemIds(), 4162));
		assertTrue(contains(reminder.getItemIds(), 21754));
	}

	@Test
	public void resolvesLitBullseyeLanternMissingFromBucketByItsGameId()
	{
		WikiRequiredItemRule rule = new WikiRequiredItemRule("Cave horror", Arrays.asList(
			Collections.singletonList("Witchwood icon"),
			Collections.singletonList("Bullseye lantern (lit)")));
		String json = "{\"bucket\":["
			+ "{\"item_name\":\"Witchwood icon\",\"item_id\":[\"8923\"]}]}";

		Map<String, List<ReminderItem>> result = WikiRequiredItemResolver.resolve(
			Collections.singletonList(rule), new JsonParser().parse(json).getAsJsonObject());

		List<ReminderItem> reminders = result.get("cave horror");
		assertEquals(2, reminders.size());
		assertEquals("Bullseye lantern (lit)", reminders.get(1).getName());
		assertTrue(contains(reminders.get(1).getItemIds(), ItemID.BULLSEYE_LANTERN_LIT));
		assertFalse(contains(reminders.get(1).getItemIds(), ItemID.BULLSEYE_LANTERN_UNLIT));
	}

	@Test
	public void rejectsCompoundRuleWhenAnyGroupCannotBeResolved()
	{
		WikiRequiredItemRule rule = new WikiRequiredItemRule("Warped Terrorbird", Arrays.asList(
			Collections.singletonList("Crystal chime"),
			Arrays.asList("Earmuffs", "Slayer helmet")));
		String json = "{\"bucket\":[{\"item_name\":\"Crystal chime\",\"item_id\":[\"28577\"]}]}";

		Map<String, List<ReminderItem>> result = WikiRequiredItemResolver.resolve(
			Collections.singletonList(rule), new JsonParser().parse(json).getAsJsonObject());

		assertFalse(result.containsKey("warped terrorbird"));
	}

	private static int occurrences(String value, String needle)
	{
		return (value.length() - value.replace(needle, "").length()) / needle.length();
	}

	private static boolean contains(int[] values, int expected)
	{
		return Arrays.stream(values).anyMatch(value -> value == expected);
	}
}
