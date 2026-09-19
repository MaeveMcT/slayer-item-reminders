package com.slayeritemreminders;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SlayerItemRemindersStartupTest
{
	@Test
	public void doesNotShowReminderWhenEnabledDuringExistingTask() throws Exception
	{
		Harness harness = createHarness(GameState.LOGGED_IN);

		harness.plugin.startUp();

		verifyNoInteractions(harness.infoBoxManager);
	}

	@Test
	public void showsRequiredReminderWhenTaskIsChecked() throws Exception
	{
		Harness harness = createHarness(GameState.LOGIN_SCREEN);
		harness.plugin.startUp();
		GameStateChanged loggedIn = new GameStateChanged();
		loggedIn.setGameState(GameState.LOGGED_IN);
		harness.plugin.onGameStateChanged(loggedIn);
		verifyNoInteractions(harness.infoBoxManager);

		Widget widget = mock(Widget.class);
		when(widget.getItemId()).thenReturn(ItemID.SLAYER_GEM);
		when(widget.getDynamicChildren()).thenReturn(new Widget[0]);
		when(harness.client.getWidget(123)).thenReturn(widget);
		MenuEntry menuEntry = mock(MenuEntry.class);
		when(menuEntry.getType()).thenReturn(MenuAction.CC_OP);
		when(menuEntry.getOption()).thenReturn("Check");
		when(menuEntry.getParam0()).thenReturn(-1);
		when(menuEntry.getParam1()).thenReturn(123);

		harness.plugin.onMenuOptionClicked(new MenuOptionClicked(menuEntry));

		verify(harness.infoBoxManager, atLeastOnce()).addInfoBox(any(ReminderInfoBox.class));
	}

	@Test
	public void doesNotShowRecommendationsPlayerDoesNotOwn() throws Exception
	{
		Harness harness = createHarness(GameState.LOGIN_SCREEN);
		harness.plugin.startUp();
		logInAndCheckTask(harness);

		verify(harness.infoBoxManager, never()).addInfoBox(argThat(infoBox ->
			java.awt.Color.YELLOW.equals(infoBox.getTextColor())));
	}

	@Test
	public void onlyShowsRecommendationsPlayerOwns() throws Exception
	{
		Harness harness = createHarness(GameState.LOGIN_SCREEN, true, false);
		harness.plugin.startUp();
		logInAndCheckTask(harness);

		verify(harness.infoBoxManager).addInfoBox(argThat(infoBox ->
			java.awt.Color.YELLOW.equals(infoBox.getTextColor())
				&& "1".equals(infoBox.getText())
				&& infoBox.getTooltip().contains("Herb sack")
				&& !infoBox.getTooltip().contains("Seed box")));
	}

	@Test
	public void reminderWindowLastsTenSeconds()
	{
		assertEquals(java.time.Duration.ofSeconds(10), SlayerItemRemindersPlugin.REMINDER_DURATION);
	}

	@Test
	public void doesNotOpenVariantPanelWhenEnabledDuringExistingTask() throws Exception
	{
		Harness harness = createHarness(GameState.LOGGED_IN);

		harness.plugin.startUp();
		SwingUtilities.invokeAndWait(() -> { });

		verify(harness.clientToolbar, never()).openPanel(any());
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

		verify(harness.infoBoxManager, atLeastOnce()).addInfoBox(any(ReminderInfoBox.class));
	}

	private static void logInAndCheckTask(Harness harness)
	{
		GameStateChanged loggedIn = new GameStateChanged();
		loggedIn.setGameState(GameState.LOGGED_IN);
		harness.plugin.onGameStateChanged(loggedIn);

		Widget widget = mock(Widget.class);
		when(widget.getItemId()).thenReturn(ItemID.SLAYER_GEM);
		when(widget.getDynamicChildren()).thenReturn(new Widget[0]);
		when(harness.client.getWidget(123)).thenReturn(widget);
		MenuEntry menuEntry = mock(MenuEntry.class);
		when(menuEntry.getType()).thenReturn(MenuAction.CC_OP);
		when(menuEntry.getOption()).thenReturn("Check");
		when(menuEntry.getParam0()).thenReturn(-1);
		when(menuEntry.getParam1()).thenReturn(123);
		harness.plugin.onMenuOptionClicked(new MenuOptionClicked(menuEntry));
	}

	private static Harness createHarness(GameState initialState) throws Exception
	{
		return createHarness(initialState, false, false);
	}

	private static Harness createHarness(GameState initialState, boolean hasHerbSack,
		boolean hasSeedBox) throws Exception
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
		WikiDropTableClient wikiDropTableClient = mock(WikiDropTableClient.class);
		doAnswer(invocation ->
		{
			invocation.<Consumer<Set<RecommendedItem>>>getArgument(1).accept(
				EnumSet.allOf(RecommendedItem.class));
			return null;
		}).when(wikiDropTableClient).lookup(any(String.class), any());
		WikiTaskVariantClient wikiTaskVariantClient = mock(WikiTaskVariantClient.class);
		doAnswer(invocation ->
		{
			invocation.<Consumer<java.util.List<TaskVariant>>>getArgument(1).accept(Arrays.asList(
				new TaskVariant("Gargoyle", "Gargoyle"),
				new TaskVariant("Dusk", "Dusk")));
			return null;
		}).when(wikiTaskVariantClient).lookup(any(String.class), any());
		SlayerItemRemindersConfig config = mock(SlayerItemRemindersConfig.class);
		when(config.currentTaskVariant()).thenReturn("");
		when(config.hasHerbSack()).thenReturn(hasHerbSack);
		when(config.hasSeedBox()).thenReturn(hasSeedBox);

		SlayerItemRemindersPlugin plugin = new SlayerItemRemindersPlugin();
		inject(plugin, "client", client);
		inject(plugin, "clientThread", clientThread);
		inject(plugin, "itemManager", itemManager);
		inject(plugin, "infoBoxManager", infoBoxManager);
		inject(plugin, "wikiDropTableClient", wikiDropTableClient);
		inject(plugin, "wikiTaskVariantClient", wikiTaskVariantClient);
		inject(plugin, "wikiRequiredItemClient", mock(WikiRequiredItemClient.class));
		inject(plugin, "config", config);
		inject(plugin, "configManager", mock(ConfigManager.class));
		ClientToolbar clientToolbar = mock(ClientToolbar.class);
		inject(plugin, "clientToolbar", clientToolbar);
		inject(plugin, "panel", new SlayerItemRemindersPanel());
		return new Harness(plugin, client, infoBoxManager, clientToolbar);
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
		private final Client client;
		private final InfoBoxManager infoBoxManager;
		private final ClientToolbar clientToolbar;

		private Harness(SlayerItemRemindersPlugin plugin, Client client,
			InfoBoxManager infoBoxManager, ClientToolbar clientToolbar)
		{
			this.plugin = plugin;
			this.client = client;
			this.infoBoxManager = infoBoxManager;
			this.clientToolbar = clientToolbar;
		}
	}
}
