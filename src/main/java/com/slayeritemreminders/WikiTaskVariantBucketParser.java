package com.slayeritemreminders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class WikiTaskVariantBucketParser
{
	private static final String[] EXCLUDED_CONTEXTS = {
		"(pvm arena)", "(deadman)", "(echo)"
	};

	private WikiTaskVariantBucketParser()
	{
	}

	static List<TaskVariant> parse(JsonObject response)
	{
		JsonArray rows = response.getAsJsonArray("bucket");
		if (rows == null)
		{
			return Collections.emptyList();
		}

		Map<String, TaskVariant> variants = new LinkedHashMap<>();
		for (JsonElement element : rows)
		{
			if (!element.isJsonObject())
			{
				continue;
			}
			JsonObject row = element.getAsJsonObject();
			if (!row.has("name") || !row.has("page_name"))
			{
				continue;
			}

			String name = row.get("name").getAsString().trim();
			String wikiPage = row.get("page_name").getAsString().trim();
			if (name.isEmpty() || wikiPage.isEmpty() || isExcluded(name) || isExcluded(wikiPage))
			{
				continue;
			}
			variants.putIfAbsent(wikiPage.toLowerCase(Locale.ENGLISH),
				new TaskVariant(name, wikiPage));
		}
		return Collections.unmodifiableList(new ArrayList<>(variants.values()));
	}

	private static boolean isExcluded(String value)
	{
		String normalized = value.toLowerCase(Locale.ENGLISH);
		for (String context : EXCLUDED_CONTEXTS)
		{
			if (normalized.contains(context))
			{
				return true;
			}
		}
		return false;
	}
}
