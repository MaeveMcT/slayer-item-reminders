package com.slayeritemreminders;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
final class TaskVariant
{
	private final String name;
	private final String wikiPage;
	private final List<ReminderItem> requiredItems;
	private final String eligibilityNote;

	TaskVariant(String name, String wikiPage, List<ReminderItem> requiredItems)
	{
		this(name, wikiPage, requiredItems, null);
	}

	TaskVariant(String name, String wikiPage, List<ReminderItem> requiredItems, String eligibilityNote)
	{
		this.name = name;
		this.wikiPage = wikiPage;
		this.requiredItems = Collections.unmodifiableList(requiredItems);
		this.eligibilityNote = eligibilityNote;
	}

	TaskVariant(String name, String wikiPage)
	{
		this(name, wikiPage, Collections.emptyList());
	}
}
