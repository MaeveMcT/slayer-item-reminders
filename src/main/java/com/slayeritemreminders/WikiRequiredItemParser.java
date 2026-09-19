package com.slayeritemreminders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WikiRequiredItemParser
{
	private static final int MONSTER_COLUMN = 1;
	private static final int REQUIRED_ITEMS_COLUMN = 6;
	private static final Pattern ROW = Pattern.compile("(?m)^\\|-\\s*$");
	private static final Pattern ROWSPAN = Pattern.compile("(?i)rowspan\\s*=\\s*\"?(\\d+)");
	private static final Pattern LINK_TEMPLATE = Pattern.compile(
		"(?i)\\{\\{(?:ilinkt|ilink|plinkt|plinkp|plink)\\|([^}|]+)");

	private WikiRequiredItemParser()
	{
	}

	static List<WikiRequiredItemRule> parse(String wikiText)
	{
		List<WikiRequiredItemRule> rules = new ArrayList<>();
		Map<Integer, Span> spans = new HashMap<>();
		for (String row : ROW.split(wikiText))
		{
			Map<Integer, String> logicalCells = new HashMap<>();
			Map<Integer, Span> nextSpans = new HashMap<>();
			for (Map.Entry<Integer, Span> entry : spans.entrySet())
			{
				logicalCells.put(entry.getKey(), entry.getValue().value);
				if (entry.getValue().remainingRows > 1)
				{
					nextSpans.put(entry.getKey(), new Span(
						entry.getValue().value, entry.getValue().remainingRows - 1));
				}
			}

			int column = 0;
			for (Cell cell : parseCells(row))
			{
				while (logicalCells.containsKey(column))
				{
					column++;
				}
				logicalCells.put(column, cell.value);
				if (cell.rowspan > 1)
				{
					nextSpans.put(column, new Span(cell.value, cell.rowspan - 1));
				}
				column++;
			}
			spans = nextSpans;

			String monster = firstLink(logicalCells.get(MONSTER_COLUMN));
			List<String> items = allLinks(logicalCells.get(REQUIRED_ITEMS_COLUMN));
			if (monster == null || items.isEmpty())
			{
				continue;
			}
			rules.add(new WikiRequiredItemRule(monster, groupRequirements(monster, items)));
		}
		return Collections.unmodifiableList(rules);
	}

	private static List<List<String>> groupRequirements(String monster, List<String> items)
	{
		String key = monster.toLowerCase(Locale.ENGLISH);
		if (key.equals("warped terrorbird"))
		{
			return Arrays.asList(
				matching(items, "Crystal chime"),
				matching(items, "Earmuffs", "Slayer helmet"));
		}
		if (key.equals("cave horror"))
		{
			return Arrays.asList(
				matching(items, "Witchwood icon"),
				matching(items, "Bullseye lantern (lit)"));
		}
		if (key.equals("mutated zygomite") || key.equals("ancient zygomite"))
		{
			return Collections.singletonList(matching(items, "Fungicide spray"));
		}
		return Collections.singletonList(items);
	}

	private static List<String> matching(List<String> items, String... accepted)
	{
		Set<String> acceptedNames = new LinkedHashSet<>(Arrays.asList(accepted));
		List<String> result = new ArrayList<>();
		for (String item : items)
		{
			if (acceptedNames.contains(item))
			{
				result.add(item);
			}
		}
		return result;
	}

	private static List<Cell> parseCells(String row)
	{
		List<Cell> cells = new ArrayList<>();
		StringBuilder current = null;
		int rowspan = 1;
		for (String line : row.split("\\R"))
		{
			if (line.startsWith("|") && !line.startsWith("|}"))
			{
				if (current != null)
				{
					cells.add(new Cell(current.toString().trim(), rowspan));
				}
				Matcher spanMatcher = ROWSPAN.matcher(line);
				rowspan = spanMatcher.find() ? Integer.parseInt(spanMatcher.group(1)) : 1;
				current = new StringBuilder(stripCellAttributes(line.substring(1)));
			}
			else if (current != null)
			{
				current.append('\n').append(line);
			}
		}
		if (current != null)
		{
			cells.add(new Cell(current.toString().trim(), rowspan));
		}
		return cells;
	}

	private static String stripCellAttributes(String cell)
	{
		String trimmed = cell.trim();
		String lower = trimmed.toLowerCase(Locale.ENGLISH);
		if (lower.startsWith("rowspan") || lower.startsWith("colspan")
			|| lower.startsWith("style") || lower.startsWith("class"))
		{
			int separator = trimmed.indexOf('|');
			if (separator >= 0)
			{
				return trimmed.substring(separator + 1).trim();
			}
		}
		return trimmed;
	}

	private static String firstLink(String value)
	{
		List<String> links = allLinks(value);
		return links.isEmpty() ? null : links.get(0);
	}

	private static List<String> allLinks(String value)
	{
		if (value == null)
		{
			return Collections.emptyList();
		}
		Map<String, String> links = new LinkedHashMap<>();
		Matcher matcher = LINK_TEMPLATE.matcher(value);
		while (matcher.find())
		{
			String name = matcher.group(1).replace('_', ' ').trim();
			links.putIfAbsent(name.toLowerCase(Locale.ENGLISH), name);
		}
		return new ArrayList<>(links.values());
	}

	private static final class Cell
	{
		private final String value;
		private final int rowspan;

		private Cell(String value, int rowspan)
		{
			this.value = value;
			this.rowspan = rowspan;
		}
	}

	private static final class Span
	{
		private final String value;
		private final int remainingRows;

		private Span(String value, int remainingRows)
		{
			this.value = value;
			this.remainingRows = remainingRows;
		}
	}
}
