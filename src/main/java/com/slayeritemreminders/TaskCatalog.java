package com.slayeritemreminders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

final class TaskCatalog
{
	private static final Map<String, TaskDefinition> TASKS;

	static
	{
		Map<String, TaskDefinition> tasks = new HashMap<>();
		tasks.put("gargoyles", new TaskDefinition("Gargoyle", Collections.singletonList(
			new ReminderItem("Rock hammer or rock thrownhammer",
				ItemID.SLAYER_ROCK_HAMMER, ItemID.SLAYER_ROCK_THROWNHAMMER))));
		tasks.put("desert lizards", new TaskDefinition("Desert lizard", Collections.singletonList(
			new ReminderItem("Ice cooler", ItemID.SLAYER_ICY_WATER))));
		TASKS = Collections.unmodifiableMap(tasks);
	}

	private TaskCatalog()
	{
	}

	static TaskDefinition get(String taskName)
	{
		return TASKS.get(taskName.toLowerCase(Locale.ENGLISH));
	}
}
