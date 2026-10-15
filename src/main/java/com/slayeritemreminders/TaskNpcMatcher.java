package com.slayeritemreminders;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.runelite.client.util.Text;

/** Exact normalized names: broad substring matching can confuse unrelated monsters. */
final class TaskNpcMatcher
{
	private final Set<String> names = new HashSet<>();

	TaskNpcMatcher(String task, Collection<TaskVariant> variants)
	{
		if (task != null)
		{
			WikiTaskVariantClient.taskPageCandidates(task).forEach(this::add);
			TaskDefinition definition = TaskCatalog.get(task);
			if (definition != null)
			{
				definition.getVariants().forEach(this::add);
			}
		}
		variants.forEach(this::add);
	}

	boolean matches(String npcName)
	{
		return npcName != null && names.contains(normalize(npcName));
	}

	private void add(TaskVariant variant)
	{
		add(variant.getName());
		add(variant.getWikiPage());
	}

	private void add(String name)
	{
		names.add(normalize(name));
	}

	static String normalize(String name)
	{
		return Text.removeTags(name).replace('_', ' ').replace('\u00a0', ' ')
			.replace('\u2019', '\'').replaceAll("\\s*\\([^)]*\\)$", "")
			.trim().toLowerCase(Locale.ENGLISH);
	}
}
