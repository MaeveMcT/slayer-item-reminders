package com.slayeritemreminders;

public enum TaskVariantChoice
{
	AUTOMATIC("Automatic (task default)", null, null),
	GREATER_DEMON("Greater demon", "Greater demons", "Greater demon"),
	TORMENTED_DEMON("Tormented Demon", "Greater demons", "Tormented Demon"),
	KRIL_TSUTSAROTH("K'ril Tsutsaroth", "Greater demons", "K'ril Tsutsaroth"),
	SKOTIZO("Skotizo", "Greater demons", "Skotizo");

	private final String displayName;
	private final String taskName;
	private final String variantName;

	TaskVariantChoice(String displayName, String taskName, String variantName)
	{
		this.displayName = displayName;
		this.taskName = taskName;
		this.variantName = variantName;
	}

	TaskVariant resolve(String currentTaskName, TaskDefinition definition)
	{
		if (taskName == null || !taskName.equalsIgnoreCase(currentTaskName) || definition == null)
		{
			return null;
		}

		return definition.getVariants().stream()
			.filter(variant -> variantName.equals(variant.getName()))
			.findFirst()
			.orElse(null);
	}

	static TaskVariantChoice from(String currentTaskName, TaskVariant variant)
	{
		for (TaskVariantChoice choice : values())
		{
			if (choice.taskName != null
				&& choice.taskName.equalsIgnoreCase(currentTaskName)
				&& choice.variantName.equals(variant.getName()))
			{
				return choice;
			}
		}
		return AUTOMATIC;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
