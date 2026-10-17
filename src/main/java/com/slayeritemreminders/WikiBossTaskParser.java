package com.slayeritemreminders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads explicit, directional substitutes, not every link in a boss's table row. */
final class WikiBossTaskParser
{
	private static final Pattern SECTION = Pattern.compile(
		"(?ims)^==\\s*Assignable bosses\\s*==\\s*$([\\s\\S]*?)(?=^==[^=]|\\z)");
	private static final Pattern ROW = Pattern.compile("(?m)^\\|-\\s*$");
	private static final Pattern FIRST_CELL = Pattern.compile("(?ms)^\\|\\s*(.*?)(?=^\\||^!|^\\|})");
	private static final Pattern BOSS = Pattern.compile("(?i)^\\s*\\{\\{ilinkt\\|([^|}]+)(?:\\|[^}]*?)?}}");
	private static final Pattern SUBSTITUTE = Pattern.compile(
		"(?is)<sub>\\s*\\(\\s*\\[\\[([^]#|]+)(?:\\|([^]]+))?]]\\s+(?:can|may)\\s+be\\s+killed\\s+instead\\s*\\)\\s*</sub>");

	private WikiBossTaskParser()
	{
	}

	static Map<String, List<TaskVariant>> parse(String wikiText)
	{
		Matcher section = SECTION.matcher(wikiText);
		if (!section.find())
		{
			throw new IllegalArgumentException("Boss Slayer assignable table missing");
		}
		Map<String, List<TaskVariant>> bosses = new LinkedHashMap<>();
		for (String row : ROW.split(section.group(1)))
		{
			Matcher cell = FIRST_CELL.matcher(row);
			if (!cell.find())
			{
				continue;
			}
			Matcher primary = BOSS.matcher(cell.group(1));
			if (!primary.find())
			{
				continue;
			}
			String boss = primary.group(1).trim().replace('_', ' ');
			Map<String, TaskVariant> variants = new LinkedHashMap<>();
			variants.put(key(boss), new TaskVariant(boss, boss));
			Matcher substitute = SUBSTITUTE.matcher(cell.group(1));
			while (substitute.find())
			{
				String page = substitute.group(1).trim().replace('_', ' ');
				String name = substitute.group(2) == null ? page : substitute.group(2).trim();
				// Reviewed exception from Boss Slayer's Overview. Pair discovery stays data-driven;
				// conditional eligibility is not inferred from arbitrary Wiki prose.
				String restriction = key(page).equals("deranged archaeologist")
					? "Not for Konar assignments" : null;
				variants.putIfAbsent(key(page), new TaskVariant(name, page, Collections.emptyList(), restriction));
			}
			bosses.put(key(boss), Collections.unmodifiableList(new ArrayList<>(variants.values())));
		}
		if (bosses.isEmpty())
		{
			throw new IllegalArgumentException("Boss Slayer assignable table unreadable");
		}
		return Collections.unmodifiableMap(bosses);
	}

	static String key(String name)
	{
		return name.replace('_', ' ').trim().toLowerCase(Locale.ENGLISH);
	}
}
