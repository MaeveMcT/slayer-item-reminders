package com.slayeritemreminders;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TaskVariantChoiceTest
{
	@Test
	public void resolvesVariantForMatchingTask()
	{
		TaskDefinition definition = TaskCatalog.get("Greater demons");

		TaskVariant variant = TaskVariantChoice.TORMENTED_DEMON.resolve("Greater demons", definition);

		assertEquals("Tormented Demon", variant.getName());
		assertEquals(TaskVariantChoice.TORMENTED_DEMON,
			TaskVariantChoice.from("Greater demons", variant));
	}

	@Test
	public void ignoresVariantForDifferentTask()
	{
		assertNull(TaskVariantChoice.SKOTIZO.resolve("Gargoyles", TaskCatalog.get("Gargoyles")));
	}

	@Test
	public void automaticUsesTaskDefault()
	{
		assertNull(TaskVariantChoice.AUTOMATIC.resolve(
			"Greater demons", TaskCatalog.get("Greater demons")));
	}
}
