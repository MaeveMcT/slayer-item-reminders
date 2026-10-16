package com.slayeritemreminders;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Notification;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class StoredTaskReminderTest
{
	private final Client client = mock(Client.class);
	private final ConfigManager configManager = mock(ConfigManager.class);
	private final SlayerItemRemindersConfig config = mock(SlayerItemRemindersConfig.class);
	private final WikiTaskVariantClient variants = mock(WikiTaskVariantClient.class);
	private final InfoBoxManager infoBoxes = mock(InfoBoxManager.class);
	private final Notifier notifier = mock(Notifier.class);
	private final SlayerItemRemindersPanel panel = mock(SlayerItemRemindersPanel.class);
	private final Player player = mock(Player.class);
	private final NPC npc = mock(NPC.class);
	private final List<InfoBox> boxes = new ArrayList<>();
	private StoredTaskReminder reminder;

	@Before
	public void setUp()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getVarbitValue(VarbitID.SLAYER_UNLOCK_STORAGE)).thenReturn(1);
		when(configManager.getRSProfileKey()).thenReturn("profile-a");
		when(config.storedTaskGameMessage()).thenReturn(true);
		when(config.storedTaskNotification()).thenReturn(Notification.OFF);
		when(infoBoxes.getInfoBoxes()).thenAnswer(i -> new ArrayList<>(boxes));
		doAnswer(i -> { boxes.add(i.getArgument(0)); return null; }).when(infoBoxes).addInfoBox(any());
		doAnswer(i -> { boxes.remove(i.getArgument(0)); return null; }).when(infoBoxes).removeInfoBox(any());
		doAnswer(i ->
		{
			i.<Consumer<TaskVariantDiscoveryResult>>getArgument(1).accept(
				TaskVariantDiscoveryResult.loaded(Collections.singletonList(new TaskVariant("Vorkath", "Vorkath"))));
			return null;
		}).when(variants).lookup(anyString(), any());
		NPCComposition composition = mock(NPCComposition.class);
		when(composition.getActions()).thenReturn(new String[]{"Attack", null});
		when(npc.getTransformedComposition()).thenReturn(composition);
		when(npc.getName()).thenReturn("Vorkath");
		reminder = new StoredTaskReminder(client, configManager, config, variants,
			infoBoxes, notifier, panel, new Gson());
		reminder.start(mock(Plugin.class));
	}

	private void saved(String name)
	{
		when(configManager.getRSProfileConfiguration(SlayerItemRemindersConfig.GROUP,
			StoredTaskReminder.STORAGE_KEY)).thenReturn("{\"name\":\"" + name + "\",\"amount\":120}");
	}

	private void tick(String current, String... names)
	{
		List<TaskVariant> discovered = new ArrayList<>();
		for (String name : names)
		{
			discovered.add(new TaskVariant(name, name));
		}
		reminder.tick(current, discovered, true);
	}

	private void engage()
	{
		reminder.onInteractingChanged(new InteractingChanged(player, npc));
	}

	@Test
	public void warnsForBossVariantAndRateLimitsAcrossMonsters()
	{
		saved("Blue dragons");
		tick("Gargoyles", "Gargoyle");
		engage();
		engage();
		verify(client, times(1)).addChatMessage(eq(ChatMessageType.GAMEMESSAGE), eq(""), contains("Blue dragons"), isNull());
		when(client.getTickCount()).thenReturn(100);
		engage();
		verify(client, times(2)).addChatMessage(any(), anyString(), anyString(), isNull());
	}

	@Test
	public void suppressesIdenticalTasksAndOverlappingBoss()
	{
		saved("Blue dragons");
		tick("Blue dragons", "Vorkath");
		engage();
		tick("Zombies", "Vorkath", "Zombie");
		engage();
		verifyNoInteractions(notifier);
		verify(client, never()).addChatMessage(any(), anyString(), anyString(), any());
	}

	@Test
	public void suppressesWhileCurrentVariantsAreUnknown()
	{
		saved("Blue dragons");
		reminder.tick("Zombies", Collections.emptyList(), false);
		engage();
		verifyNoInteractions(notifier);
	}

	@Test
	public void unresolvedActiveTaskIsNotTreatedAsNoTask()
	{
		saved("Blue dragons");
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(100);
		tick(null);
		engage();
		verifyNoInteractions(notifier);
	}

	@Test
	public void warnsWithoutAnActiveTaskAndHonorsMessageToggle()
	{
		saved("Blue dragons");
		when(config.storedTaskGameMessage()).thenReturn(false);
		when(config.storedTaskNotification()).thenReturn(Notification.ON);
		tick(null);
		engage();
		verify(notifier).notify(eq(Notification.ON), contains("Blue dragons"));
		verify(client, never()).addChatMessage(any(), anyString(), anyString(), any());
	}

	@Test
	public void ignoresOtherPlayersNonMatchesAndLockedStorage()
	{
		saved("Blue dragons");
		tick("Gargoyles");
		reminder.onInteractingChanged(new InteractingChanged(mock(Player.class), npc));
		when(npc.getName()).thenReturn("Guard");
		engage();
		when(npc.getName()).thenReturn("Vorkath");
		when(client.getVarbitValue(VarbitID.SLAYER_UNLOCK_STORAGE)).thenReturn(0);
		engage();
		verifyNoInteractions(notifier);
	}

	@Test
	public void profileSwitchCannotLeakStorageOrWarnings()
	{
		saved("Blue dragons");
		tick("Gargoyles");
		when(configManager.getRSProfileKey()).thenReturn("profile-b");
		engage(); // Before the next tick, old-profile state is already unusable.
		when(configManager.getRSProfileConfiguration(anyString(), anyString())).thenReturn(null);
		tick("Gargoyles");
		assertNull(reminder.getStoredTaskName());
		assertTrue(boxes.isEmpty());
		verifyNoInteractions(notifier);
	}

	@Test
	public void unrelatedInterfaceVarpsCannotOverwriteSavedTask()
	{
		saved("Blue dragons");
		when(client.getVarpValue(VarPlayerID.IF4)).thenReturn(0);
		tick("Gargoyles");
		assertEquals("Blue dragons", reminder.getStoredTaskName());
		verify(configManager, never()).setRSProfileConfiguration(anyString(), anyString(), any());
	}

	private void openTasks(int count, int id, String name)
	{
		Widget tasks = mock(Widget.class);
		Widget content = mock(Widget.class);
		when(client.getWidget(InterfaceID.SlayerRewards.TASKS)).thenReturn(tasks);
		when(client.getWidget(InterfaceID.SlayerRewards.TASKS_CONTENT_UPPER)).thenReturn(content);
		when(content.getDynamicChildren()).thenReturn(new Widget[]{mock(Widget.class)});
		when(client.getVarpValue(VarPlayerID.IF4)).thenReturn(count);
		when(client.getVarpValue(VarPlayerID.IF5)).thenReturn(id);
		when(client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, id))
			.thenReturn(Collections.singletonList(42));
		when(client.getDBTableField(42, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0))
			.thenReturn(new Object[]{name});
	}

	@Test
	public void learnsAndPersistsStorageAndConfirmedEmptySlot()
	{
		openTasks(70, 1, "Blue dragons");
		tick("Gargoyles");
		assertEquals("Blue dragons", reminder.getStoredTaskName());
		verify(configManager).setRSProfileConfiguration(eq(SlayerItemRemindersConfig.GROUP),
			eq(StoredTaskReminder.STORAGE_KEY), contains("Blue dragons"));
		openTasks(0, 0, null);
		tick("Gargoyles");
		assertNull(reminder.getStoredTaskName());
		assertTrue(boxes.isEmpty());
		verify(panel).showStoredTask("Stored: none");
	}

	@Test
	public void uninitializedTasksInterfaceCannotConfirmAnEmptySlot()
	{
		saved("Blue dragons");
		openTasks(0, 0, null);
		when(client.getWidget(InterfaceID.SlayerRewards.TASKS_CONTENT_UPPER).getDynamicChildren())
			.thenReturn(new Widget[0]);
		tick("Gargoyles");
		assertEquals("Blue dragons", reminder.getStoredTaskName());
		verify(configManager, never()).setRSProfileConfiguration(anyString(), anyString(), any());
	}

	@Test
	public void missingDatabaseRowsDoNotEraseKnownStorage()
	{
		saved("Blue dragons");
		openTasks(100, 2, "Gargoyles");
		when(client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, 2))
			.thenReturn(Collections.emptyList());
		tick("Gargoyles");
		assertEquals("Blue dragons", reminder.getStoredTaskName());
		verify(configManager, never()).setRSProfileConfiguration(anyString(), anyString(), any());
	}

	@Test
	public void challengeCountIsNotMistakenForAnEmptySlot()
	{
		openTasks(-1, 1, "TzTok-Jad");
		tick("Gargoyles");
		assertEquals("TzTok-Jad", reminder.getStoredTaskName());
		verify(panel).showStoredTask("Stored: TzTok-Jad");
		assertTrue(boxes.isEmpty());
	}

	@Test
	public void hopClearsStoredTaskWarnings()
	{
		saved("Blue dragons");
		tick("Gargoyles");
		assertTrue(boxes.isEmpty());
		reminder.suspend();
		engage();
		verifyNoInteractions(notifier);
	}

	@Test
	public void storedTaskNeverCreatesAStandaloneCounter()
	{
		saved("Blue dragons");
		tick("Gargoyles");
		tick(null);
		verify(infoBoxes, never()).addInfoBox(any());
		verify(panel).showStoredTask("Stored: Blue dragons × 120");
	}

	@Test
	public void tooltipSurvivesSlayerRebuildAndIsRestoredOnStop()
	{
		saved("Blue dragons");
		InfoBox counter = new Counter(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), mock(Plugin.class), 10)
		{
			@Override
			public String getName()
			{
				return "SlayerPlugin_TaskCounter";
			}
		};
		counter.setTooltip("Current task");
		boxes.add(counter);
		tick("Gargoyles");
		tick("Gargoyles");
		assertEquals("Current task</br>Stored: Blue dragons × 120", counter.getTooltip());
		assertEquals(1, boxes.size());
		counter.setTooltip("Rebuilt by Slayer");
		tick("Gargoyles");
		assertEquals("Rebuilt by Slayer</br>Stored: Blue dragons × 120", counter.getTooltip());
		reminder.stop();
		assertEquals("Rebuilt by Slayer", counter.getTooltip());
	}

	@Test
	public void unknownOrCorruptStorageDoesNotAlertOrClaimEmpty()
	{
		when(configManager.getRSProfileConfiguration(anyString(), anyString())).thenReturn("not json");
		tick("Gargoyles");
		engage();
		assertNull(reminder.getStoredTaskName());
		assertTrue(boxes.isEmpty());
		verify(panel).showStoredTask(contains("unknown"));
		verifyNoInteractions(notifier);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void ignoresWikiResponseAfterShutdown()
	{
		doNothing().when(variants).lookup(anyString(), any());
		saved("Blue dragons");
		tick("Gargoyles");
		ArgumentCaptor<Consumer<TaskVariantDiscoveryResult>> callback = ArgumentCaptor.forClass(Consumer.class);
		verify(variants).lookup(eq("Blue dragons"), callback.capture());
		reminder.stop();
		callback.getValue().accept(TaskVariantDiscoveryResult.loaded(Arrays.asList(new TaskVariant("Vorkath", "Vorkath"))));
		engage();
		verifyNoInteractions(notifier);
	}
}
