package com.slayeritemreminders;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SlayerItemRemindersPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SlayerItemRemindersPlugin.class);
		RuneLite.main(args);
	}
}
