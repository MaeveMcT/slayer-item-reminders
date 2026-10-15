package com.slayeritemreminders;

import net.runelite.client.config.Notification;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SlayerItemRemindersConfigTest
{
	private final SlayerItemRemindersConfig config = new SlayerItemRemindersConfig() { };

	@Test
	public void enablesStoredTaskWarningsAndCheckInfoBoxByDefault()
	{
		assertTrue(config.storedTaskGameMessage());
		assertEquals(Notification.ON, config.storedTaskNotification());
		assertTrue(config.storedTaskInfoBox());
	}

	@Test
	public void enablesOwnedContainerRemindersByDefault()
	{
		assertTrue(config.hasHerbSack());
		assertTrue(config.hasSeedBox());
	}
}
