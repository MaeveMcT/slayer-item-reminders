package com.slayeritemreminders;

import java.time.Duration;
import java.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WikiRequestThrottleTest
{
	@Test
	public void suppressesRetriesForFiveMinutesAfterFailure()
	{
		WikiRequestThrottle throttle = new WikiRequestThrottle();
		Instant failure = Instant.parse("2026-09-07T12:00:00Z");

		throttle.recordFailure("Ankou", failure);

		assertFalse(throttle.shouldRequest("Ankou", failure.plus(Duration.ofMinutes(4))));
		assertTrue(throttle.shouldRequest("Ankou", failure.plus(Duration.ofMinutes(5))));
	}

	@Test
	public void successClearsFailureCooldown()
	{
		WikiRequestThrottle throttle = new WikiRequestThrottle();
		Instant failure = Instant.parse("2026-09-07T12:00:00Z");
		throttle.recordFailure("Ankou", failure);

		throttle.recordSuccess("Ankou");

		assertTrue(throttle.shouldRequest("Ankou", failure));
	}
}
