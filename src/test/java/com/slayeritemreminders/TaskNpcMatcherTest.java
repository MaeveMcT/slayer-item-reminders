package com.slayeritemreminders;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class TaskNpcMatcherTest
{
	@Test
	public void matchesCanonicalMonstersAndDiscoveredBosses()
	{
		TaskNpcMatcher matcher = new TaskNpcMatcher("Blue dragons", Arrays.asList(
			new TaskVariant("Baby blue dragon", "Baby blue dragon"),
			new TaskVariant("Vorkath", "Vorkath")));
		assertTrue(matcher.matches("Blue dragon"));
		assertTrue(matcher.matches("Baby blue dragon"));
		assertTrue(matcher.matches("<col=ff0000>VORKATH</col>"));
		assertFalse(matcher.matches("Black dragon"));
		assertFalse(matcher.matches("Vorkathling"));
		assertFalse(matcher.matches(null));
	}

	@Test
	public void normalizesWikiDisambiguationAndPunctuation()
	{
		TaskNpcMatcher matcher = new TaskNpcMatcher("Greater demons", Collections.singletonList(
			new TaskVariant("K’ril Tsutsaroth", "K'ril_Tsutsaroth_(monster)")));
		assertTrue(matcher.matches("K'ril Tsutsaroth"));
		assertTrue(matcher.matches("Greater demon"));
	}
}
