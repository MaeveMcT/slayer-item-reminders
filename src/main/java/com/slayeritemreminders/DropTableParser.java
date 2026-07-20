package com.slayeritemreminders;

import java.io.IOException;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

final class DropTableParser
{
	private static final int RECOMMENDATION_THRESHOLD = 4;

	private DropTableParser()
	{
	}

	static Set<RecommendedItem> parse(String html) throws IOException
	{
		DropTableCallback callback = new DropTableCallback();
		new ParserDelegator().parse(new StringReader(html), callback, true);

		EnumSet<RecommendedItem> recommendations = EnumSet.noneOf(RecommendedItem.class);
		if (callback.herbs.size() >= RECOMMENDATION_THRESHOLD)
		{
			recommendations.add(RecommendedItem.HERB_SACK);
		}
		if (callback.seeds.size() >= RECOMMENDATION_THRESHOLD)
		{
			recommendations.add(RecommendedItem.SEED_BOX);
		}
		return recommendations;
	}

	private static boolean isHerb(String item)
	{
		return item.startsWith("grimy ");
	}

	private static boolean isSeed(String item)
	{
		// TODO: Consider deriving seed box compatibility from https://oldschool.runescape.wiki/w/Seeds.
		return (item.endsWith(" seed") && !item.contains("crystal"))
			|| item.equals("acorn")
			|| item.endsWith(" spore");
	}

	private static final class DropTableCallback extends HTMLEditorKit.ParserCallback
	{
		private final Set<String> herbs = new HashSet<>();
		private final Set<String> seeds = new HashSet<>();
		private final Set<String> rowItems = new HashSet<>();
		private final StringBuilder rowText = new StringBuilder();
		private boolean inDrops;
		private boolean inRow;

		@Override
		public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position)
		{
			if (tag == HTML.Tag.H2)
			{
				inDrops = "Drops".equals(attributes.getAttribute(HTML.Attribute.ID));
			}
			else if (inDrops && tag == HTML.Tag.TR)
			{
				inRow = true;
				rowItems.clear();
				rowText.setLength(0);
			}
			else if (inDrops && inRow && tag == HTML.Tag.A)
			{
				Object title = attributes.getAttribute(HTML.Attribute.TITLE);
				if (title != null)
				{
					rowItems.add(title.toString().toLowerCase(Locale.ENGLISH));
				}
			}
		}

		@Override
		public void handleText(char[] data, int position)
		{
			if (inDrops && inRow)
			{
				rowText.append(data);
			}
		}

		@Override
		public void handleEndTag(HTML.Tag tag, int position)
		{
			if (tag == HTML.Tag.TR && inRow)
			{
				if (!rowText.toString().toLowerCase(Locale.ENGLISH).contains("noted"))
				{
					rowItems.stream().filter(DropTableParser::isHerb).forEach(herbs::add);
					rowItems.stream().filter(DropTableParser::isSeed).forEach(seeds::add);
				}
				inRow = false;
			}
		}
	}
}
