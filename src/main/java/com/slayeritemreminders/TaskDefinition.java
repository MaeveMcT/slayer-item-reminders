package com.slayeritemreminders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
final class TaskDefinition
{
	private final List<TaskVariant> variants;

	TaskDefinition(TaskVariant... variants)
	{
		if (variants.length == 0)
		{
			throw new IllegalArgumentException("A task definition requires at least one variant");
		}
		this.variants = Collections.unmodifiableList(Arrays.asList(variants));
	}

	TaskVariant getDefaultVariant()
	{
		return variants.get(0);
	}

	boolean hasMultipleVariants()
	{
		return variants.size() > 1;
	}
}
