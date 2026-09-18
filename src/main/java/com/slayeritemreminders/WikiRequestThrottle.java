package com.slayeritemreminders;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

final class WikiRequestThrottle
{
	private static final Duration FAILURE_COOLDOWN = Duration.ofMinutes(5);

	private final Map<String, Instant> failedAt = new HashMap<>();

	boolean shouldRequest(String key, Instant now)
	{
		Instant failure = failedAt.get(key);
		if (failure == null)
		{
			return true;
		}
		if (!now.isBefore(failure.plus(FAILURE_COOLDOWN)))
		{
			failedAt.remove(key);
			return true;
		}
		return false;
	}

	void recordSuccess(String key)
	{
		failedAt.remove(key);
	}

	void recordFailure(String key, Instant now)
	{
		failedAt.put(key, now);
	}

	void reset()
	{
		failedAt.clear();
	}
}
