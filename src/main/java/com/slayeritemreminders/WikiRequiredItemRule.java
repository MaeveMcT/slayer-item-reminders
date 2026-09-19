package com.slayeritemreminders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
final class WikiRequiredItemRule
{
	private final String monster;
	private final List<List<String>> requirementGroups;

	WikiRequiredItemRule(String monster, List<List<String>> requirementGroups)
	{
		this.monster = monster;
		List<List<String>> groups = new ArrayList<>();
		for (List<String> group : requirementGroups)
		{
			groups.add(Collections.unmodifiableList(new ArrayList<>(group)));
		}
		this.requirementGroups = Collections.unmodifiableList(groups);
	}
}
