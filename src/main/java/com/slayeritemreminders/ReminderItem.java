package com.slayeritemreminders;

import java.util.Arrays;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

@Getter
final class ReminderItem
{
	private final String name;
	private final int[] itemIds;

	ReminderItem(String name, int... itemIds)
	{
		this.name = name;
		this.itemIds = itemIds;
	}

	int getImageItemId()
	{
		return itemIds[0];
	}

	boolean isPresent(Client client)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		return Arrays.stream(itemIds).anyMatch(itemId -> contains(inventory, itemId) || contains(equipment, itemId));
	}

	private static boolean contains(ItemContainer container, int itemId)
	{
		return container != null && container.count(itemId) > 0;
	}
}
