package com.slayeritemreminders;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
final class TaskDefinition
{
	private final String wikiPage;
	private final List<ReminderItem> requiredItems;

	TaskDefinition(String wikiPage, List<ReminderItem> requiredItems)
	{
		this.wikiPage = wikiPage;
		this.requiredItems = Collections.unmodifiableList(requiredItems);
	}
}
