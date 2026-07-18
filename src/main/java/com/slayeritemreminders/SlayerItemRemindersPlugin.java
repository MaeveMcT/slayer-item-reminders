package com.slayeritemreminders;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Slayer Item Reminders",
	description = "Reminds you which items to bring for Slayer tasks",
	tags = {"slayer", "items", "reminders"}
)
public class SlayerItemRemindersPlugin extends Plugin
{
	@Override
	protected void startUp() throws Exception
	{
		log.debug("Slayer Item Reminders started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Slayer Item Reminders stopped");
	}
}
