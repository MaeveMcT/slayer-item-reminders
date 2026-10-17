package com.slayeritemreminders;

import java.awt.Component;
import java.awt.Container;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SlayerItemRemindersPanelTest
{
	@Test
	public void conditionalVariantShowsItsRestrictionWithoutChangingItsName() throws Exception
	{
		AtomicReference<SlayerItemRemindersPanel> reference = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() ->
		{
			SlayerItemRemindersPanel panel = new SlayerItemRemindersPanel();
			reference.set(panel);
			TaskVariant variant = new TaskVariant("Deranged archaeologist", "Deranged archaeologist",
				Collections.emptyList(), "Not for Konar assignments");
			panel.showTask("Crazy Archaeologists", Collections.singletonList(variant), variant,
				VariantLoadState.LOADED);
		});
		// showTask queues its rebuild even when called on the EDT.
		SwingUtilities.invokeAndWait(() ->
		{
			assertTrue(containsLabel(reference.get(), "Deranged archaeologist"));
			assertTrue(containsLabel(reference.get(), "Not for Konar assignments"));
		});
	}

	private static boolean containsLabel(Container container, String text)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof JLabel && ((JLabel) component).getText() != null
				&& ((JLabel) component).getText().contains(text))
			{
				return true;
			}
			if (component instanceof Container && containsLabel((Container) component, text))
			{
				return true;
			}
		}
		return false;
	}
}
