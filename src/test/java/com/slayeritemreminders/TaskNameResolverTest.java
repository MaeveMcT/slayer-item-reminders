package com.slayeritemreminders;

import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TaskNameResolverTest
{
	@Test
	public void resolvesSpecificBossInsteadOfGenericBossCategory()
	{
		Client client = mock(Client.class);
		when(client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID,
			DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, 0, 7))
			.thenReturn(Collections.singletonList(100));
		when(client.getDBTableField(100, DBTableID.SlayerTaskSublist.COL_TASK, 0))
			.thenReturn(new Object[]{200});
		when(client.getDBTableField(200, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0))
			.thenReturn(new Object[]{"Vorkath"});
		assertEquals("Vorkath", TaskNameResolver.resolve(client, 98, 7));
	}

	@Test
	public void missingRowsRemainUnknown()
	{
		Client client = mock(Client.class);
		assertNull(TaskNameResolver.resolve(client, 1, 0));
		assertNull(TaskNameResolver.resolve(client, 98, 7));
	}
}
