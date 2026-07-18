# ADR 0005: No MVP configuration

- Status: Accepted
- Date: 2026-07-18

## Context

The MVP should validate the reminder workflow before introducing customization and configuration migration obligations.

## Decision

The MVP exposes no configuration options. The following behavior is fixed:

- Five-minute reminder expiry.
- More than four distinct eligible drop-table items triggers a loot-container recommendation.
- Required and optional infobox colors and presentation.
- OSRS Wiki lookups for locally mapped non-generic tasks.
- Thirty-second Wiki request timeout.
- Reminder lifecycle and dismissal behavior.

## Consequences

- The plugin does not need a configuration interface for the MVP.
- Users cannot disable network lookups or adjust reminder behavior independently.
- Mandatory Wiki requests disclose the player's IP address to the OSRS Wiki; current Plugin Hub policy must be verified before submission.
- Future configuration additions need stable, plugin-specific keys and safe defaults.
