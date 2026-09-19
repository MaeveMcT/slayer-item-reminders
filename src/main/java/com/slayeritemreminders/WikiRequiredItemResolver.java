package com.slayeritemreminders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemVariationMapping;

final class WikiRequiredItemResolver
{
	private WikiRequiredItemResolver()
	{
	}

	static String buildBucketQuery(List<WikiRequiredItemRule> rules)
	{
		Set<String> names = new LinkedHashSet<>();
		for (WikiRequiredItemRule rule : rules)
		{
			for (List<String> group : rule.getRequirementGroups())
			{
				names.addAll(group);
			}
		}
		String conditions = names.stream()
			.map(name -> "{'item_name','" + escapeLua(name) + "'}")
			.collect(Collectors.joining(","));
		return "bucket('infobox_item').select('item_name','item_id')"
			+ ".where(bucket.Or(" + conditions + ")).limit(500).run()";
	}

	static Map<String, List<ReminderItem>> resolve(List<WikiRequiredItemRule> rules, JsonObject response)
	{
		Map<String, Set<Integer>> idsByName = parseItemIds(response);
		Map<String, List<ReminderItem>> result = new LinkedHashMap<>();
		for (WikiRequiredItemRule rule : rules)
		{
			List<ReminderItem> requirements = new ArrayList<>();
			boolean complete = true;
			for (List<String> group : rule.getRequirementGroups())
			{
				Set<Integer> ids = new LinkedHashSet<>();
				List<String> resolvedNames = new ArrayList<>();
				for (String name : group)
				{
					Set<Integer> knownIds = knownItemIds(name);
					if (!knownIds.isEmpty())
					{
						resolvedNames.add(name);
						ids.addAll(knownIds);
						continue;
					}

					Set<Integer> itemIds = idsByName.get(key(name));
					if (itemIds != null && !itemIds.isEmpty())
					{
						resolvedNames.add(name);
						for (int itemId : itemIds)
						{
							Collection<Integer> variations = ItemVariationMapping.getVariations(
								ItemVariationMapping.map(itemId));
							ids.addAll(variations);
						}
					}
				}
				if (ids.isEmpty())
				{
					complete = false;
					break;
				}
				int[] itemIds = ids.stream().mapToInt(Integer::intValue).toArray();
				requirements.add(new ReminderItem(
					String.join(" or ", resolvedNames), itemIds));
			}
			if (complete && !requirements.isEmpty())
			{
				result.put(key(rule.getMonster()),
					Collections.unmodifiableList(requirements));
			}
		}
		return Collections.unmodifiableMap(result);
	}

	private static Map<String, Set<Integer>> parseItemIds(JsonObject response)
	{
		Map<String, Set<Integer>> idsByName = new LinkedHashMap<>();
		JsonArray rows = response.getAsJsonArray("bucket");
		if (rows == null)
		{
			return idsByName;
		}
		for (JsonElement element : rows)
		{
			JsonObject row = element.getAsJsonObject();
			if (!row.has("item_name") || !row.has("item_id"))
			{
				continue;
			}
			Set<Integer> ids = idsByName.computeIfAbsent(
				key(row.get("item_name").getAsString()), ignored -> new LinkedHashSet<>());
			JsonElement idValue = row.get("item_id");
			if (idValue.isJsonArray())
			{
				for (JsonElement id : idValue.getAsJsonArray())
				{
					addId(ids, id);
				}
			}
			else
			{
				addId(ids, idValue);
			}
		}
		return idsByName;
	}

	private static Set<Integer> knownItemIds(String name)
	{
		if ("bullseye lantern (lit)".equals(key(name)))
		{
			Set<Integer> ids = new LinkedHashSet<>();
			ids.add(ItemID.BULLSEYE_LANTERN_LIT);
			ids.add(ItemID.BULLSEYE_LANTERN_LIT_LUNAR_QUEST);
			return ids;
		}
		return Collections.emptySet();
	}

	private static void addId(Set<Integer> ids, JsonElement value)
	{
		try
		{
			ids.add(Integer.parseInt(value.getAsString()));
		}
		catch (NumberFormatException ignored)
		{
			// Ignore malformed external data rather than creating an invalid item requirement.
		}
	}

	private static String escapeLua(String value)
	{
		return value.replace("\\", "\\\\").replace("'", "\\'");
	}

	private static String key(String value)
	{
		return value.toLowerCase(Locale.ENGLISH);
	}
}
