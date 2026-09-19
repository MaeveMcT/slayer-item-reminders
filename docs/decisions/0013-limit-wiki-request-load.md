# ADR 0013: Limit OSRS Wiki request load

- Status: Accepted (amended by ADR 0015)
- Date: 2026-09-07

## Context

Variant discovery and drop-table analysis use the public OSRS Wiki API. A normal new assignment needs one task-page request and one monster-page request, and a selected alternative variant may need another monster-page request. Successful results and concurrent identical lookups are already cached for the RuneLite session.

Failed requests were immediately eligible for another attempt after a bank close, and selecting another variant left a superseded drop-table request running. Neither client declared a MediaWiki `maxlag` threshold.

## Decision

- Include `maxlag=5` on variant and drop-table API requests so the Wiki can reject work while its replication lag is elevated.
- After a network, HTTP, or parsing failure, suppress another request for the same task or page for five minutes.
- Keep successful and empty-result session caching and concurrent request deduplication.
- Allow at most the currently relevant drop-table request to remain pending. Cancel a pending request when another assignment or variant supersedes it.
- Clear request cooldown state on plugin shutdown along with the existing session caches.

## Consequences

- Repeated bank closes cannot rapidly retry an unavailable Wiki page.
- Quickly changing variants does not leave every obsolete drop-table response downloading.
- A transient failure can leave recommendations unavailable for up to five minutes; required local reminders remain unaffected.
- Cancellation primarily limits unnecessary response transfer and processing; the Wiki may already have received the request.
- Persistent caching remains deferred.
