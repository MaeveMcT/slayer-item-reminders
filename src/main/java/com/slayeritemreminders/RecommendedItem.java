package com.slayeritemreminders;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

@Getter
enum RecommendedItem
{
	HERB_SACK(new ReminderItem("Herb sack",
		ItemID.SLAYER_HERB_SACK,
		ItemID.SLAYER_HERB_SACK_OPEN,
		ItemID.SLAYER_HERB_SACK_SILK,
		ItemID.SLAYER_HERB_SACK_SILK_OPEN)),
	SEED_BOX(new ReminderItem("Seed box",
		ItemID.SEED_BOX,
		ItemID.SEED_BOX_OPEN));

	private final ReminderItem reminderItem;

	RecommendedItem(ReminderItem reminderItem)
	{
		this.reminderItem = reminderItem;
	}
}
