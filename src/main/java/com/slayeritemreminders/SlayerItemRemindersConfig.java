package com.slayeritemreminders;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SlayerItemRemindersConfig.GROUP)
public interface SlayerItemRemindersConfig extends Config
{
	String GROUP = "slayer-item-reminders";
	String HERB_SACK_KEY = "hasHerbSack";
	String SEED_BOX_KEY = "hasSeedBox";
	String CURRENT_TASK_VARIANT_KEY = "currentTaskVariant";

	@ConfigItem(
		keyName = HERB_SACK_KEY,
		name = "Own herb sack",
		description = "Show herb sack reminders when it is not in your inventory",
		position = 0
	)
	default boolean hasHerbSack()
	{
		return true;
	}

	@ConfigItem(
		keyName = SEED_BOX_KEY,
		name = "Own seed box",
		description = "Show seed box reminders when it is not in your inventory",
		position = 1
	)
	default boolean hasSeedBox()
	{
		return true;
	}

	@ConfigItem(
		keyName = CURRENT_TASK_VARIANT_KEY,
		name = "Current task variant",
		description = "The assignment-scoped variant selected in the Slayer Item Reminders panel",
		position = 2,
		hidden = true
	)
	default String currentTaskVariant()
	{
		return "";
	}
}
