package com.slayeritemreminders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
final class TaskVariantDiscoveryResult
{
	private final List<TaskVariant> variants;
	private final boolean unavailable;

	private TaskVariantDiscoveryResult(List<TaskVariant> variants, boolean unavailable)
	{
		this.variants = Collections.unmodifiableList(new ArrayList<>(variants));
		this.unavailable = unavailable;
	}

	static TaskVariantDiscoveryResult loaded(List<TaskVariant> variants)
	{
		return new TaskVariantDiscoveryResult(variants, false);
	}

	static TaskVariantDiscoveryResult unavailable()
	{
		return new TaskVariantDiscoveryResult(Collections.emptyList(), true);
	}
}
