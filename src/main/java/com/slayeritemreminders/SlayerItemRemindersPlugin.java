package com.slayeritemreminders;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import com.google.inject.Provides;
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
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.InfoBoxMenuClicked;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

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
	private WikiTaskVariantClient wikiTaskVariantClient;

	@Inject
	private WikiRequiredItemClient wikiRequiredItemClient;

	@Inject
	private SlayerItemRemindersConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private SlayerItemRemindersPanel panel;

	private NavigationButton navigationButton;
	private String taskName;
	private boolean suppressTaskReminder;
	private boolean bankOpen;
	private boolean reminderWindowActive;
	private boolean dismissed;
	private boolean optionalOverrideVisible;
	private long taskGeneration;
	private TaskVariant selectedVariant;
	private List<TaskVariant> availableVariants = Collections.emptyList();
	private boolean variantsLoading;
	private Instant reminderExpiresAt;
	private Set<RecommendedItem> recommendations = Collections.emptySet();
	private Map<String, List<ReminderItem>> wikiRequiredItems = Collections.emptyMap();
	private ReminderInfoBox requiredInfoBox;
	private ReminderInfoBox optionalInfoBox;

	@Override
	protected void startUp()
	{
		panel.setSelectionHandler((task, variant) ->
			clientThread.invoke(() -> selectVariantFromPanel(task, variant)));
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panel_icon.png");
		navigationButton = NavigationButton.builder()
			.tooltip("Slayer Item Reminders")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		updateVariantPanel();
		log.debug("Slayer Item Reminders started");
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() -> updateTaskInternal(false));
		}
	}

	@Override
	protected void shutDown()
	{
		clearAssignment();
		clientToolbar.removeNavigation(navigationButton);
		navigationButton = null;
		panel.setSelectionHandler((task, variant) -> { });
		wikiDropTableClient.reset();
		wikiTaskVariantClient.reset();
		wikiRequiredItemClient.reset();
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
	public void onConfigChanged(ConfigChanged event)
	{
		if (!SlayerItemRemindersConfig.GROUP.equals(event.getGroup())
			|| !SlayerItemRemindersConfig.CURRENT_TASK_VARIANT_KEY.equals(event.getKey())
			|| taskName == null)
		{
			return;
		}

		TaskDefinition definition = TaskCatalog.get(taskName);
		applyVariantSelection(resolveConfiguredVariant(definition));
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
		updateTaskInternal(true);
	}

	private void updateTaskInternal(boolean openVariantPanel)
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
			resetVariantConfig();
			availableVariants = getCuratedOrFallbackVariants(taskName);
			variantsLoading = false;
			updateVariantPanel();
			recommendations = Collections.emptySet();
			optionalOverrideVisible = false;
			removeInfoBoxes();
			if (!suppressTaskReminder)
			{
				activateReminderWindow(false);
				requestVariants(openVariantPanel, false);
			}
			else
			{
				requestVariants(false, false);
			}
			requestRequiredItems();
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

		if (offerVariantSelection && selectedVariant == null)
		{
			taskGeneration++;
			recommendations = Collections.emptySet();
			refreshInfoBoxes();
			requestVariants(true, true);
			return;
		}

		refreshInfoBoxes();
		requestRecommendations();
	}

	private void requestVariants(boolean openPanelWhenAmbiguous,
		boolean requestRecommendationsWhenUnambiguous)
	{
		wikiTaskVariantClient.cancelPendingExcept(taskName);
		variantsLoading = true;
		updateVariantPanel();
		String requestedTask = taskName;
		long requestedGeneration = taskGeneration;
		wikiTaskVariantClient.lookup(requestedTask, wikiVariants ->
		{
			if (!Objects.equals(taskName, requestedTask) || taskGeneration != requestedGeneration)
			{
				return;
			}

			TaskDefinition definition = mergeVariants(requestedTask, wikiVariants);
			availableVariants = definition.getVariants();
			variantsLoading = false;
			updateVariantPanel();
			if (openPanelWhenAmbiguous && definition.hasMultipleVariants() && selectedVariant == null)
			{
				SwingUtilities.invokeLater(() ->
				{
					if (navigationButton != null)
					{
						clientToolbar.openPanel(navigationButton);
					}
				});
			}
			else if (requestRecommendationsWhenUnambiguous)
			{
				requestRecommendations();
			}
		});
	}

	private List<TaskVariant> getCuratedOrFallbackVariants(String currentTask)
	{
		TaskDefinition curated = TaskCatalog.get(currentTask);
		if (curated != null)
		{
			return curated.getVariants();
		}
		return Collections.singletonList(new TaskVariant(currentTask, currentTask));
	}

	private TaskDefinition mergeVariants(String currentTask, List<TaskVariant> wikiVariants)
	{
		Map<String, TaskVariant> variants = new LinkedHashMap<>();
		TaskDefinition curated = TaskCatalog.get(currentTask);
		if (curated == null)
		{
			TaskVariant fallback = new TaskVariant(currentTask, currentTask);
			variants.put(currentTask.toLowerCase(Locale.ENGLISH), fallback);
		}
		else
		{
			for (TaskVariant variant : curated.getVariants())
			{
				variants.put(variant.getWikiPage().toLowerCase(Locale.ENGLISH), variant);
			}
		}
		for (TaskVariant variant : wikiVariants)
		{
			variants.putIfAbsent(variant.getWikiPage().toLowerCase(Locale.ENGLISH), variant);
		}
		return new TaskDefinition(variants.values().toArray(new TaskVariant[0]));
	}

	private void selectVariantFromPanel(String requestedTask, TaskVariant variant)
	{
		if (!Objects.equals(taskName, requestedTask))
		{
			return;
		}
		if (variant == null)
		{
			applyVariantSelection(null);
			resetVariantConfig();
			return;
		}

		applyVariantSelection(variant);
		if (!variant.getWikiPage().equals(config.currentTaskVariant()))
		{
			configManager.setConfiguration(
				SlayerItemRemindersConfig.GROUP,
				SlayerItemRemindersConfig.CURRENT_TASK_VARIANT_KEY,
				variant.getWikiPage());
		}
	}

	private void applyVariantSelection(TaskVariant variant)
	{
		if ((selectedVariant == null && variant == null)
			|| (selectedVariant != null && variant != null
				&& selectedVariant.getWikiPage().equalsIgnoreCase(variant.getWikiPage())))
		{
			return;
		}

		selectedVariant = variant;
		updateVariantPanel();
		taskGeneration++;
		recommendations = Collections.emptySet();
		optionalOverrideVisible = false;
		refreshInfoBoxes();
		if (reminderWindowActive)
		{
			requestRecommendations();
		}
	}

	private TaskVariant resolveConfiguredVariant(TaskDefinition definition)
	{
		String configured = config.currentTaskVariant().trim();
		if (configured.isEmpty())
		{
			return null;
		}
		if (definition != null)
		{
			for (TaskVariant variant : definition.getVariants())
			{
				if (configured.equalsIgnoreCase(variant.getName())
					|| configured.equalsIgnoreCase(variant.getWikiPage()))
				{
					return variant;
				}
			}
		}
		return new TaskVariant(configured, configured);
	}

	private void updateVariantPanel()
	{
		panel.showTask(taskName, availableVariants, selectedVariant, variantsLoading);
	}

	private void resetVariantConfig()
	{
		if (!config.currentTaskVariant().isEmpty())
		{
			configManager.setConfiguration(
				SlayerItemRemindersConfig.GROUP,
				SlayerItemRemindersConfig.CURRENT_TASK_VARIANT_KEY,
				"");
		}
	}

	private void requestRequiredItems()
	{
		wikiRequiredItemClient.lookup(result ->
		{
			wikiRequiredItems = result;
			refreshInfoBoxes();
		});
	}

	private void requestRecommendations()
	{
		TaskDefinition definition = TaskCatalog.get(taskName);
		TaskVariant variant = getActiveVariant(definition);
		String wikiPage = variant == null ? taskName : variant.getWikiPage();

		long requestedGeneration = taskGeneration;
		wikiDropTableClient.cancelPendingExcept(wikiPage);
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
		if (selectedVariant != null)
		{
			return selectedVariant;
		}
		return definition == null ? null : definition.getDefaultVariant();
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
			missingRequired = getRequiredItems(variant).stream()
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

	private List<ReminderItem> getRequiredItems(TaskVariant variant)
	{
		List<ReminderItem> wikiItems = wikiRequiredItems.get(
			variant.getWikiPage().toLowerCase(Locale.ENGLISH));
		return wikiItems == null || wikiItems.isEmpty()
			? variant.getRequiredItems() : wikiItems;
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
		String target = selectedVariant != null && variant != null
			? taskName + " (" + variant.getName() + ")"
			: taskName;
		return category + " for " + target + "<br>" + itemLines;
	}

	private void clearAssignment()
	{
		taskName = null;
		resetVariantConfig();
		taskGeneration++;
		selectedVariant = null;
		availableVariants = Collections.emptyList();
		variantsLoading = false;
		updateVariantPanel();
		recommendations = Collections.emptySet();
		wikiDropTableClient.cancelPendingExcept(null);
		wikiTaskVariantClient.cancelPendingExcept(null);
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

	@Provides
	SlayerItemRemindersConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(SlayerItemRemindersConfig.class);
	}
}
