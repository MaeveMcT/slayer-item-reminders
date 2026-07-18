package com.slayeritemreminders;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
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

	private static final Set<String> HERBS = names(
		"Grimy guam leaf", "Grimy marrentill", "Grimy tarromin", "Grimy harralander",
		"Grimy ranarr weed", "Grimy toadflax", "Grimy irit leaf", "Grimy avantoe",
		"Grimy kwuarm", "Grimy snapdragon", "Grimy cadantine", "Grimy lantadyme",
		"Grimy dwarf weed", "Grimy torstol");

	private static final Set<String> SEEDS = names(
		"Potato seed", "Onion seed", "Cabbage seed", "Tomato seed", "Sweetcorn seed",
		"Strawberry seed", "Watermelon seed", "Snape grass seed", "Marigold seed",
		"Rosemary seed", "Nasturtium seed", "Woad seed", "Limpwurt seed", "White lily seed",
		"Barley seed", "Hammerstone seed", "Asgarnian seed", "Jute seed", "Yanillian seed",
		"Krandorian seed", "Wildblood seed", "Guam seed", "Marrentill seed", "Tarromin seed",
		"Harralander seed", "Ranarr seed", "Toadflax seed", "Irit seed", "Avantoe seed",
		"Kwuarm seed", "Snapdragon seed", "Cadantine seed", "Lantadyme seed",
		"Dwarf weed seed", "Torstol seed", "Redberry seed", "Cadavaberry seed",
		"Dwellberry seed", "Jangerberry seed", "Whiteberry seed", "Poison ivy seed",
		"Acorn", "Willow seed", "Maple seed", "Yew seed", "Magic seed", "Apple tree seed",
		"Banana tree seed", "Orange tree seed", "Curry tree seed", "Pineapple seed",
		"Papaya tree seed", "Palm tree seed", "Dragonfruit tree seed", "Calquat tree seed",
		"Spirit seed", "Teak seed", "Mahogany seed", "Celastrus seed", "Redwood tree seed",
		"Seaweed spore", "Grape seed");

	private DropTableParser()
	{
	}

	static Set<RecommendedItem> parse(String html) throws IOException
	{
		DropTableCallback callback = new DropTableCallback();
		new ParserDelegator().parse(new StringReader(html), callback, true);

		EnumSet<RecommendedItem> recommendations = EnumSet.noneOf(RecommendedItem.class);
		if (callback.herbs.size() > RECOMMENDATION_THRESHOLD)
		{
			recommendations.add(RecommendedItem.HERB_SACK);
		}
		if (callback.seeds.size() > RECOMMENDATION_THRESHOLD)
		{
			recommendations.add(RecommendedItem.SEED_BOX);
		}
		return recommendations;
	}

	private static Set<String> names(String... names)
	{
		Set<String> result = new HashSet<>();
		Arrays.stream(names)
			.map(name -> name.toLowerCase(Locale.ENGLISH))
			.forEach(result::add);
		return result;
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
					rowItems.stream().filter(HERBS::contains).forEach(herbs::add);
					rowItems.stream().filter(SEEDS::contains).forEach(seeds::add);
				}
				inRow = false;
			}
		}
	}
}
