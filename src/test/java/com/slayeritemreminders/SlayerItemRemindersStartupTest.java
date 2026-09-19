package com.slayeritemreminders;

import java.lang.reflect.Field;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SlayerItemRemindersStartupTest
{
	@Test
	public void showsRequiredReminderWhenEnabledDuringExistingTask() throws Exception
	{
		Harness harness = createHarness(GameState.LOGGED_IN);

		harness.plugin.startUp();

		verify(harness.infoBoxManager).addInfoBox(any(ReminderInfoBox.class));
	}

	@Test
	public void showsRequiredReminderAfterBankClosesForSilentlySynchronizedTask() throws Exception
	{
		Harness harness = createHarness(GameState.LOGIN_SCREEN);
		harness.plugin.startUp();
		GameStateChanged loggedIn = new GameStateChanged();
		loggedIn.setGameState(GameState.LOGGED_IN);
		harness.plugin.onGameStateChanged(loggedIn);
		verifyNoInteractions(harness.infoBoxManager);

		WidgetLoaded bankLoaded = new WidgetLoaded();
		bankLoaded.setGroupId(InterfaceID.BANKMAIN);
		harness.plugin.onWidgetLoaded(bankLoaded);
		harness.plugin.onWidgetClosed(new WidgetClosed(InterfaceID.BANKMAIN, 0, true));

		verify(harness.infoBoxManager).addInfoBox(any(ReminderInfoBox.class));
	}

	private static Harness createHarness(GameState initialState) throws Exception
	{
		Client client = mock(Client.class);
		when(client.getGameState()).thenReturn(initialState);
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(100);
		when(client.getVarpValue(VarPlayerID.SLAYER_TARGET)).thenReturn(1);
		when(client.getDBRowsByValue(
			DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, 1))
			.thenReturn(Collections.singletonList(42));
		when(client.getDBTableField(42, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0))
			.thenReturn(new Object[]{"Gargoyles"});

		ClientThread clientThread = mock(ClientThread.class);
		doAnswer(invocation ->
		{
			invocation.<Runnable>getArgument(0).run();
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));

		ItemManager itemManager = mock(ItemManager.class);
		when(itemManager.getImage(anyInt())).thenReturn(mock(AsyncBufferedImage.class));
		InfoBoxManager infoBoxManager = mock(InfoBoxManager.class);
		SlayerItemRemindersConfig config = mock(SlayerItemRemindersConfig.class);
		when(config.currentTaskVariant()).thenReturn("");

		SlayerItemRemindersPlugin plugin = new SlayerItemRemindersPlugin();
		inject(plugin, "client", client);
		inject(plugin, "clientThread", clientThread);
		inject(plugin, "itemManager", itemManager);
		inject(plugin, "infoBoxManager", infoBoxManager);
		inject(plugin, "wikiDropTableClient", mock(WikiDropTableClient.class));
		inject(plugin, "wikiTaskVariantClient", mock(WikiTaskVariantClient.class));
		inject(plugin, "wikiRequiredItemClient", mock(WikiRequiredItemClient.class));
		inject(plugin, "config", config);
		inject(plugin, "configManager", mock(ConfigManager.class));
		inject(plugin, "clientToolbar", mock(ClientToolbar.class));
		inject(plugin, "panel", new SlayerItemRemindersPanel());
		return new Harness(plugin, infoBoxManager);
	}

	private static void inject(Object target, String fieldName, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class Harness
	{
		private final SlayerItemRemindersPlugin plugin;
		private final InfoBoxManager infoBoxManager;

		private Harness(SlayerItemRemindersPlugin plugin, InfoBoxManager infoBoxManager)
		{
			this.plugin = plugin;
			this.infoBoxManager = infoBoxManager;
		}
	}
}
