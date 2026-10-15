package com.slayeritemreminders;

import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;

final class TaskNameResolver
{
	private TaskNameResolver()
	{
	}

	static String resolve(Client client, int taskId, int bossId)
	{
		int row;
		if (taskId == 98) // The game's boss-assignment category, not an NPC ID.
		{
			List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID,
				DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, 0, bossId);
			if (rows.isEmpty())
			{
				return null;
			}
			row = (Integer) client.getDBTableField(rows.get(0),
				DBTableID.SlayerTaskSublist.COL_TASK, 0)[0];
		}
		else
		{
			List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID,
				DBTableID.SlayerTask.COL_ID, 0, taskId);
			if (rows.isEmpty())
			{
				return null;
			}
			row = rows.get(0);
		}
		return (String) client.getDBTableField(row,
			DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0)[0];
	}
}
