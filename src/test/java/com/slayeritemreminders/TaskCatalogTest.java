package com.slayeritemreminders;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TaskCatalogTest
{
	@Test
	public void greaterDemonsOffersCuratedVariants()
	{
		TaskDefinition definition = TaskCatalog.get("Greater demons");

		assertNotNull(definition);
		assertTrue(definition.hasMultipleVariants());
		assertEquals("Greater demon", definition.getDefaultVariant().getName());
		assertEquals(4, definition.getVariants().size());
		assertEquals("Tormented Demon", definition.getVariants().get(1).getWikiPage());
	}

	@Test
	public void gargoylesRetainsRequiredItemRule()
	{
		TaskDefinition definition = TaskCatalog.get("Gargoyles");

		assertNotNull(definition);
		assertFalse(definition.hasMultipleVariants());
		assertEquals(1, definition.getDefaultVariant().getRequiredItems().size());
	}
}
