package com.slayeritemreminders;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.InfoBoxMenuClicked;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.game.chatbox.ChatboxTextMenuInput;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

@Slf4j
@PluginDescriptor(
	name = "Slayer Item Reminders",
	description = "Reminds you which items to bring for Slayer tasks",
	tags = {"slayer", "items", "reminders"}
)
public class SlayerItemRemindersPlugin extends Plugin
{
	private static final Duration REMINDER_DURATION = Duration.ofMinutes(5);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private WikiDropTableClient wikiDropTableClient;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	private String taskName;
	private boolean suppressTaskReminder;
	private boolean bankOpen;
	private boolean reminderWindowActive;
	private boolean dismissed;
	private boolean optionalOverrideVisible;
	private boolean variantMenuOpen;
	private long taskGeneration;
	private TaskVariant selectedVariant;
	private Instant reminderExpiresAt;
	private Set<RecommendedItem> recommendations = Collections.emptySet();
	private ReminderInfoBox requiredInfoBox;
	private ReminderInfoBox optionalInfoBox;

	@Override
	protected void startUp()
	{
		log.debug("Slayer Item Reminders started");
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			synchronizeTaskSilently();
		}
	}

	@Override
	protected void shutDown()
	{
		clearAssignment();
		wikiDropTableClient.reset();
		bankOpen = false;
		suppressTaskReminder = false;
		log.debug("Slayer Item Reminders stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case HOPPING:
			case LOGGING_IN:
			case CONNECTION_LOST:
				suppressTaskReminder = true;
				bankOpen = false;
				removeInfoBoxes();
				break;
			case LOGGED_IN:
				synchronizeTaskSilently();
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int varpId = event.getVarpId();
		int varbitId = event.getVarbitId();
		if (varpId == VarPlayerID.SLAYER_COUNT
			|| varpId == VarPlayerID.SLAYER_AREA
			|| varpId == VarPlayerID.SLAYER_TARGET
			|| varbitId == VarbitID.SLAYER_TARGET_BOSSID
			|| varpId == VarPlayerID.SLAYER_COUNT_ORIGINAL)
		{
			clientThread.invokeLater(this::updateTask);
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (isBankGroup(event.getGroupId()))
		{
			bankOpen = true;
			removeInfoBoxes();
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.isUnload() && isBankGroup(event.getGroupId()))
		{
			bankOpen = false;
			updateTask();
			if (taskName != null)
			{
				activateReminderWindow(true);
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		ItemContainer changed = event.getItemContainer();
		if (changed == client.getItemContainer(InventoryID.INV)
			|| changed == client.getItemContainer(InventoryID.WORN))
		{
			clientThread.invokeLater(this::refreshInfoBoxes);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if ((reminderWindowActive || optionalOverrideVisible) && reminderExpiresAt != null
			&& !Instant.now().isBefore(reminderExpiresAt))
		{
			reminderWindowActive = false;
			optionalOverrideVisible = false;
			removeInfoBoxes();
		}
	}

	@Subscribe
	public void onInfoBoxMenuClicked(InfoBoxMenuClicked event)
	{
		if ((event.getInfoBox() == requiredInfoBox || event.getInfoBox() == optionalInfoBox)
			&& ReminderInfoBox.DISMISS.equals(event.getEntry().getOption()))
		{
			dismissed = true;
			reminderWindowActive = false;
			optionalOverrideVisible = false;
			removeInfoBoxes();
		}
	}

	private void synchronizeTaskSilently()
	{
		suppressTaskReminder = true;
		clientThread.invokeLater(() ->
		{
			updateTask();
			suppressTaskReminder = false;
		});
	}

	private void updateTask()
	{
		int amount = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
		if (amount <= 0)
		{
			if (taskName != null)
			{
				clearAssignment();
			}
			return;
		}

		String currentTaskName = readTaskName();
		if (currentTaskName == null)
		{
			return;
		}

		boolean changed = !Objects.equals(taskName, currentTaskName);
		taskName = currentTaskName;
		if (changed)
		{
			taskGeneration++;
			selectedVariant = null;
			variantMenuOpen = false;
			recommendations = Collections.emptySet();
			optionalOverrideVisible = false;
			removeInfoBoxes();
			if (!suppressTaskReminder)
			{
				activateReminderWindow(false);
			}
		}
	}

	private String readTaskName()
	{
		int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
		int taskRow;
		if (taskId == 98)
		{
			List<Integer> bossRows = client.getDBRowsByValue(
				DBTableID.SlayerTaskSublist.ID,
				DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
				0,
				client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
			if (bossRows.isEmpty())
			{
				return null;
			}
			taskRow = (Integer) client.getDBTableField(
				bossRows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0)[0];
		}
		else
		{
			List<Integer> taskRows = client.getDBRowsByValue(
				DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, taskId);
			if (taskRows.isEmpty())
			{
				return null;
			}
			taskRow = taskRows.get(0);
		}

		return (String) client.getDBTableField(
			taskRow, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0)[0];
	}

	private void activateReminderWindow(boolean offerVariantSelection)
	{
		dismissed = false;
		optionalOverrideVisible = false;
		reminderWindowActive = true;
		reminderExpiresAt = Instant.now().plus(REMINDER_DURATION);

		TaskDefinition definition = TaskCatalog.get(taskName);
		if (offerVariantSelection && definition != null
			&& definition.hasMultipleVariants() && selectedVariant == null)
		{
			taskGeneration++;
			recommendations = Collections.emptySet();
			refreshInfoBoxes();
			openVariantMenu(definition);
			return;
		}

		refreshInfoBoxes();
		requestRecommendations();
	}

	private void openVariantMenu(TaskDefinition definition)
	{
		if (variantMenuOpen)
		{
			return;
		}

		variantMenuOpen = true;
		String requestedTask = taskName;
		long requestedGeneration = taskGeneration;
		ChatboxTextMenuInput input = chatboxPanelManager.openTextMenuInput("Choose monster for " + taskName);
		for (TaskVariant variant : definition.getVariants())
		{
			input.option(variant.getName(), () -> selectVariant(
				requestedTask, requestedGeneration, variant));
		}
		input.onClose(() -> variantMenuOpen = false).build();
	}

	private void selectVariant(String requestedTask, long requestedGeneration, TaskVariant variant)
	{
		if (!Objects.equals(taskName, requestedTask) || taskGeneration != requestedGeneration)
		{
			return;
		}

		selectedVariant = variant;
		taskGeneration++;
		recommendations = Collections.emptySet();
		optionalOverrideVisible = false;
		refreshInfoBoxes();
		requestRecommendations();
	}

	private void requestRecommendations()
	{
		TaskDefinition definition = TaskCatalog.get(taskName);
		TaskVariant variant = getActiveVariant(definition);
		String wikiPage = variant == null ? taskName : variant.getWikiPage();

		long requestedGeneration = taskGeneration;
		wikiDropTableClient.lookup(wikiPage, result ->
		{
			if (taskName == null || taskGeneration != requestedGeneration)
			{
				return;
			}

			recommendations = result;
			if (dismissed && !result.isEmpty())
			{
				optionalOverrideVisible = true;
			}
			refreshInfoBoxes();
		});
	}

	private TaskVariant getActiveVariant(TaskDefinition definition)
	{
		if (definition == null)
		{
			return null;
		}
		return selectedVariant == null ? definition.getDefaultVariant() : selectedVariant;
	}

	private void refreshInfoBoxes()
	{
		if (taskName == null || bankOpen)
		{
			removeInfoBoxes();
			return;
		}

		TaskDefinition definition = TaskCatalog.get(taskName);
		TaskVariant variant = getActiveVariant(definition);
		List<ReminderItem> missingRequired = Collections.emptyList();
		if (!dismissed && reminderWindowActive && variant != null)
		{
			missingRequired = variant.getRequiredItems().stream()
				.filter(item -> !item.isPresent(client))
				.collect(Collectors.toList());
		}
		updateInfoBox(true, missingRequired);

		List<ReminderItem> missingOptional = Collections.emptyList();
		if ((!dismissed && reminderWindowActive) || optionalOverrideVisible)
		{
			missingOptional = recommendations.stream()
				.map(RecommendedItem::getReminderItem)
				.filter(item -> !item.isPresent(client))
				.collect(Collectors.toList());
		}
		updateInfoBox(false, missingOptional);
	}

	private void updateInfoBox(boolean required, List<ReminderItem> missingItems)
	{
		ReminderInfoBox current = required ? requiredInfoBox : optionalInfoBox;
		if (current != null)
		{
			infoBoxManager.removeInfoBox(current);
			if (required)
			{
				requiredInfoBox = null;
			}
			else
			{
				optionalInfoBox = null;
			}
		}

		if (missingItems.isEmpty())
		{
			return;
		}

		ReminderItem first = missingItems.get(0);
		ReminderInfoBox infoBox = new ReminderInfoBox(
			itemManager.getImage(first.getImageItemId()),
			this,
			missingItems.size(),
			required ? Color.RED : Color.YELLOW,
			buildTooltip(required ? "Required" : "Recommended", missingItems));
		infoBoxManager.addInfoBox(infoBox);
		if (required)
		{
			requiredInfoBox = infoBox;
		}
		else
		{
			optionalInfoBox = infoBox;
		}
	}

	private String buildTooltip(String category, List<ReminderItem> items)
	{
		String itemLines = items.stream()
			.map(item -> item.getName())
			.collect(Collectors.joining("<br>"));
		TaskDefinition definition = TaskCatalog.get(taskName);
		TaskVariant variant = getActiveVariant(definition);
		String target = definition != null && definition.hasMultipleVariants() && variant != null
			? taskName + " (" + variant.getName() + ")"
			: taskName;
		return category + " for " + target + "<br>" + itemLines;
	}

	private void clearAssignment()
	{
		taskName = null;
		taskGeneration++;
		selectedVariant = null;
		variantMenuOpen = false;
		recommendations = Collections.emptySet();
		dismissed = false;
		optionalOverrideVisible = false;
		reminderWindowActive = false;
		reminderExpiresAt = null;
		removeInfoBoxes();
	}

	private void removeInfoBoxes()
	{
		if (requiredInfoBox != null)
		{
			infoBoxManager.removeInfoBox(requiredInfoBox);
			requiredInfoBox = null;
		}
		if (optionalInfoBox != null)
		{
			infoBoxManager.removeInfoBox(optionalInfoBox);
			optionalInfoBox = null;
		}
	}

	private static boolean isBankGroup(int groupId)
	{
		return groupId == InterfaceID.BANKMAIN || groupId == InterfaceID.BANK_DEPOSITBOX;
	}
}
