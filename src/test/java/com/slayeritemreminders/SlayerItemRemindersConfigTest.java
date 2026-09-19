package com.slayeritemreminders;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SlayerItemRemindersConfigTest
{
	private final SlayerItemRemindersConfig config = new SlayerItemRemindersConfig() { };

	@Test
	public void enablesOwnedContainerRemindersByDefault()
	{
		assertTrue(config.hasHerbSack());
		assertTrue(config.hasSeedBox());
	}
}
