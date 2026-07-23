package com.slayeritemreminders;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SlayerItemRemindersConfig.GROUP)
public interface SlayerItemRemindersConfig extends Config
{
	String GROUP = "slayer-item-reminders";
	String CURRENT_TASK_VARIANT_KEY = "currentTaskVariant";

	@ConfigItem(
		keyName = CURRENT_TASK_VARIANT_KEY,
		name = "Current task variant",
		description = "The assignment-scoped variant selected in the Slayer Item Reminders panel",
		position = 0,
		hidden = true
	)
	default String currentTaskVariant()
	{
		return "";
	}
}
