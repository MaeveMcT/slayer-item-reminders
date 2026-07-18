package com.slayeritemreminders;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collections;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.infobox.InfoBox;

final class ReminderInfoBox extends InfoBox
{
	static final String DISMISS = "Dismiss";

	private final int count;
	private final Color textColor;

	ReminderInfoBox(BufferedImage image, SlayerItemRemindersPlugin plugin, int count, Color textColor, String tooltip)
	{
		super(image, plugin);
		this.count = count;
		this.textColor = textColor;
		setTooltip(tooltip);
		setMenuEntries(Collections.singletonList(new OverlayMenuEntry(
			MenuAction.RUNELITE_INFOBOX, DISMISS, "Slayer Item Reminders")));
	}

	@Override
	public String getText()
	{
		return Integer.toString(count);
	}

	@Override
	public Color getTextColor()
	{
		return textColor;
	}
}
