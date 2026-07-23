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
		description = "Select the monster you intend to kill for the current Slayer assignment",
		position = 0
	)
	default TaskVariantChoice currentTaskVariant()
	{
		return TaskVariantChoice.AUTOMATIC;
	}
}
