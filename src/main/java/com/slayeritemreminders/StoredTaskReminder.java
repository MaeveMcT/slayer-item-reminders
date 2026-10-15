package com.slayeritemreminders;

import com.google.gson.Gson;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

/** Owns profile-scoped storage knowledge, presentation, and engagement warning policy. */
final class StoredTaskReminder
{
	static final String STORAGE_KEY = "storedAssignment";
	private static final int ALERT_COOLDOWN_TICKS = 100;

	private final Client client;
	private final ConfigManager configManager;
	private final SlayerItemRemindersConfig config;
	private final WikiTaskVariantClient variants;
	private final InfoBoxManager infoBoxes;
	private final ItemManager itemManager;
	private final Notifier notifier;
	private final SlayerItemRemindersPanel panel;
	private final Gson gson;
	private final Map<InfoBox, String> decorated = new IdentityHashMap<>();
	private Plugin owner;
	private String profile;
	private Assignment stored;
	private String currentTask;
	private TaskNpcMatcher storedMatcher;
	private TaskNpcMatcher currentMatcher;
	private List<TaskVariant> matchedCurrentVariants;
	private String panelText;
	private boolean currentVariantsReady;
	private int generation;
	private int nextAlertTick;
	private Counter storedInfoBox;
	private Instant checkExpiresAt;

	@Inject
	StoredTaskReminder(Client client, ConfigManager configManager, SlayerItemRemindersConfig config,
		WikiTaskVariantClient variants, InfoBoxManager infoBoxes, ItemManager itemManager,
		Notifier notifier, SlayerItemRemindersPanel panel, Gson gson)
	{
		this.client = client;
		this.configManager = configManager;
		this.config = config;
		this.variants = variants;
		this.infoBoxes = infoBoxes;
		this.itemManager = itemManager;
		this.notifier = notifier;
		this.panel = panel;
		this.gson = gson;
	}

	void start(Plugin owner)
	{
		this.owner = owner;
	}

	void stop()
	{
		suspend();
		owner = null;
	}

	void suspend()
	{
		generation++;
		profile = null;
		stored = null;
		storedMatcher = null;
		currentMatcher = null;
		currentTask = null;
		nextAlertTick = 0;
		checkExpiresAt = null;
		clearPresentation();
		setPanelText(null);
	}

	String getStoredTaskName()
	{
		return stored == null ? null : stored.name;
	}

	void tick(String currentTask, List<TaskVariant> currentVariants, boolean variantsReady)
	{
		if (owner == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		String key = configManager.getRSProfileKey();
		if (!Objects.equals(profile, key))
		{
			suspend();
			profile = key;
			if (profile != null)
			{
				String saved = configManager.getRSProfileConfiguration(SlayerItemRemindersConfig.GROUP, STORAGE_KEY);
				try
				{
					Assignment restored = gson.fromJson(saved, Assignment.class);
					if (restored != null && restored.amount >= -1)
					{
						setStored(restored);
					}
				}
				catch (RuntimeException ignored)
				{
					// Old/corrupt local data is unknown, never evidence of an empty slot.
				}
			}
		}
		if (profile == null)
		{
			return;
		}
		if (!Objects.equals(this.currentTask, currentTask) || matchedCurrentVariants != currentVariants
			|| currentMatcher == null)
		{
			currentMatcher = new TaskNpcMatcher(currentTask, currentVariants);
			matchedCurrentVariants = currentVariants;
		}
		this.currentTask = currentTask;
		currentVariantsReady = variantsReady
			&& (currentTask != null || client.getVarpValue(VarPlayerID.SLAYER_COUNT) == 0);
		boolean unlocked = client.getVarbitValue(VarbitID.SLAYER_UNLOCK_STORAGE) != 0;
		if (unlocked)
		{
			observeRewards();
		}
		present(unlocked);
	}

	void onTaskChecked()
	{
		if (owner != null && client.getGameState() == GameState.LOGGED_IN)
		{
			checkExpiresAt = Instant.now().plus(SlayerItemRemindersPlugin.REMINDER_DURATION);
		}
	}

	private void observeRewards()
	{
		Widget tasks = client.getWidget(InterfaceID.SlayerRewards.TASKS);
		Widget content = client.getWidget(InterfaceID.SlayerRewards.TASKS_CONTENT_UPPER);
		if (tasks == null || tasks.isHidden() || content == null
			|| content.getDynamicChildren().length == 0)
		{
			return;
		}
		// These generic interface varps are ONLY meaningful in the initialized Tasks UI.
		// See cs2 script9736: IF4=count, IF5=task category, IF6=boss subtask.
		int amount = client.getVarpValue(VarPlayerID.IF4);
		int id = client.getVarpValue(VarPlayerID.IF5);
		boolean hasTask = id != 0 && (amount > 0 || amount == -1);
		String name = hasTask ? TaskNameResolver.resolve(client, id,
			client.getVarpValue(VarPlayerID.IF6)) : null;
		if (hasTask && name == null)
		{
			return; // Database not ready: don't erase known storage.
		}
		Assignment observed = new Assignment(name, name == null ? 0 : amount);
		if (!observed.equals(stored))
		{
			setStored(observed);
			configManager.setRSProfileConfiguration(SlayerItemRemindersConfig.GROUP,
				STORAGE_KEY, gson.toJson(observed));
		}
	}

	private void setStored(Assignment assignment)
	{
		boolean changed = !Objects.equals(getStoredTaskName(), assignment.name);
		stored = assignment;
		if (!changed)
		{
			return;
		}
		int requestedGeneration = ++generation;
		nextAlertTick = 0;
		storedMatcher = null;
		if (assignment.name != null)
		{
			variants.lookup(assignment.name, result ->
			{
				if (owner != null && generation == requestedGeneration)
				{
					storedMatcher = new TaskNpcMatcher(assignment.name, result.getVariants());
				}
			});
		}
	}

	void onInteractingChanged(InteractingChanged event)
	{
		if (client.getGameState() != GameState.LOGGED_IN
			|| !Objects.equals(profile, configManager.getRSProfileKey())
			|| client.getVarbitValue(VarbitID.SLAYER_UNLOCK_STORAGE) == 0
			|| event.getSource() != client.getLocalPlayer() || !(event.getTarget() instanceof NPC)
			|| storedMatcher == null || !currentVariantsReady || stored == null
			|| (currentTask == null && client.getVarpValue(VarPlayerID.SLAYER_COUNT) != 0)
			|| (currentTask != null && TaskNpcMatcher.normalize(currentTask).equals(TaskNpcMatcher.normalize(stored.name)))
			|| client.getTickCount() < nextAlertTick)
		{
			return;
		}
		NPC npc = (NPC) event.getTarget();
		NPCComposition composition = npc.getTransformedComposition();
		if (composition == null || composition.getActions() == null
			|| !java.util.Arrays.asList(composition.getActions()).contains("Attack")
			|| !storedMatcher.matches(npc.getName())
			|| (currentTask != null && currentMatcher.matches(npc.getName())))
		{
			return;
		}
		String remaining = stored.amount < 0 ? "" : " (" + stored.amount + " remaining)";
		String message = "Stored Slayer task: " + stored.name + remaining
			+ ". You are fighting " + npc.getName() + ".";
		if (config.storedTaskGameMessage())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
		}
		notifier.notify(config.storedTaskNotification(), message);
		nextAlertTick = client.getTickCount() + ALERT_COOLDOWN_TICKS;
	}

	private void present(boolean unlocked)
	{
		restoreTooltips();
		if (!unlocked)
		{
			removeStoredInfoBox();
			setPanelText("Task storage is locked");
			return;
		}
		String text = stored == null ? "Stored: unknown — open Slayer Rewards → Tasks"
			: stored.name == null ? "Stored: none" : "Stored: " + stored.name + (stored.amount < 0 ? "" : " × " + stored.amount);
		setPanelText(text);
		for (InfoBox box : infoBoxes.getInfoBoxes())
		{
			if ("SlayerPlugin_TaskCounter".equals(box.getName()) && box.render())
			{
				String suffix = "</br>" + text;
				String tooltip = box.getTooltip();
				box.setTooltip((tooltip == null ? "" : tooltip) + suffix);
				decorated.put(box, suffix);
			}
		}
		if (config.storedTaskInfoBox() && checkExpiresAt != null && Instant.now().isBefore(checkExpiresAt)
			&& stored != null && stored.name != null)
		{
			if (storedInfoBox == null)
			{
				storedInfoBox = new Counter(itemManager.getImage(ItemID.SLAYER_GEM), owner, stored.amount)
				{
					@Override
					public String getText()
					{
						return getCount() < 0 ? "S" : super.getText();
					}
				};
				infoBoxes.addInfoBox(storedInfoBox);
			}
			storedInfoBox.setCount(stored.amount);
			storedInfoBox.setTooltip(text);
		}
		else
		{
			removeStoredInfoBox();
		}
	}

	private void setPanelText(String text)
	{
		if (!Objects.equals(panelText, text))
		{
			panelText = text;
			panel.showStoredTask(text);
		}
	}

	private void clearPresentation()
	{
		restoreTooltips();
		removeStoredInfoBox();
	}

	private void restoreTooltips()
	{
		decorated.forEach((box, suffix) ->
		{
			String tooltip = box.getTooltip();
			if (tooltip != null && tooltip.endsWith(suffix))
			{
				box.setTooltip(tooltip.substring(0, tooltip.length() - suffix.length()));
			}
		});
		decorated.clear();
	}

	private void removeStoredInfoBox()
	{
		if (storedInfoBox != null)
		{
			infoBoxes.removeInfoBox(storedInfoBox);
			storedInfoBox = null;
		}
	}

	@Value
	private static class Assignment
	{
		String name;
		int amount;
	}
}
