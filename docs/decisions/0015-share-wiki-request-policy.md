# ADR 0015: Share Wiki request policy without global scheduling

- Status: Accepted
- Date: 2026-09-16

## Context

The drop-table, task-variant, and centralized required-item clients independently implemented HTTP calls. They used the same timeout, User-Agent, `maxlag`, and cancellation conventions, but duplicated policy could drift.

A global queue, scheduler, request spacing, and persistent failure-cooldown state were considered. The plugin's requests are low-volume, user-triggered, non-critical, and already cached and deduplicated by semantic key. That machinery would add more lifecycle and concurrency risk than the traffic warrants.

## Decision

- Route every OSRS Wiki call through one singleton `WikiRequestManager` adapter.
- Keep domain clients responsible only for URL parameters, response interpretation, session caches, in-flight listener deduplication, and supersession.
- Apply the User-Agent, 30-second timeout, and `maxlag=5` centrally.
- Reject responses larger than two megabytes before JSON parsing.
- Parse JSON and deliver success or failure callbacks on the RuneLite client thread.
- Return a cancellable handle for each request and suppress callbacks after cancellation.
- Cancel superseded task-variant and drop-table requests and cancel all client-owned handles during plugin shutdown.
- Let each domain client translate request failure into its own safe result. Required-item and recommendation lookups use empty results; variant discovery reports an unavailable state so the sidebar can distinguish failure from a genuinely empty result.
- Do not maintain a failure cooldown. A later user action may retry a failed non-critical lookup.
- Do not add a global scheduler, concurrency queue, or success-rate limiter unless observed traffic demonstrates a need.

## Consequences

- Shared transport safeguards have one implementation and one test surface.
- The adapter is stateless apart from each call's cancellation handle and requires no executor lifecycle.
- The injected OkHttp dispatcher controls ordinary HTTP concurrency.
- Successful session caching and in-flight deduplication remain the primary load controls.
- Repeated user actions can retry failures, but normal task and bank interactions produce low request volume.
