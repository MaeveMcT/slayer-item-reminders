package com.slayeritemreminders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WikiTaskVariantParser
{
	private static final Pattern VARIANT_SECTION = Pattern.compile(
		"(?ims)^==\\s*Monster variants\\s*==\\s*$([\\s\\S]*?)(?=^==[^=]|\\z)");
	private static final Pattern ROW = Pattern.compile("(?m)^\\|-\\s*$");
	private static final Pattern FIRST_CELL_LINK = Pattern.compile(
		"(?m)^\\|\\s*\\[\\[([^]#|]+)(?:#[^]|]*)?(?:\\|([^]]+))?]]");

	private WikiTaskVariantParser()
	{
	}

	static List<TaskVariant> parse(String wikiText)
	{
		Matcher sectionMatcher = VARIANT_SECTION.matcher(wikiText);
		if (!sectionMatcher.find())
		{
			return Collections.emptyList();
		}

		Map<String, TaskVariant> variants = new LinkedHashMap<>();
		for (String row : ROW.split(sectionMatcher.group(1)))
		{
			Matcher linkMatcher = FIRST_CELL_LINK.matcher(row);
			if (!linkMatcher.find())
			{
				continue;
			}

			String wikiPage = normalize(linkMatcher.group(1));
			String alias = linkMatcher.group(2);
			String name = alias == null ? wikiPage : normalize(alias);
			variants.putIfAbsent(wikiPage.toLowerCase(Locale.ENGLISH), new TaskVariant(name, wikiPage));
		}
		return Collections.unmodifiableList(new ArrayList<>(variants.values()));
	}

	private static String normalize(String value)
	{
		return value.replace('_', ' ').trim();
	}
}
