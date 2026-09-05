# Experiment 0024: Vaccine Equipment Monitoring Requirements Pilot

Status: Complete

Date begun: 2026-08-30

Roadmap task: [TC-1003](../../roadmap/closed/task-1003-execute-vaccine-monitoring-requirements-pilot.yaml)

## Purpose

This experiment executes a bounded systems-engineering requirements workflow
for a hypothetical Service-Point Vaccine Equipment Monitoring System (SVEMS).
It tests whether the project's current source model and separate tools remain
useful when applied together to realistic public health cold-chain needs.

The experiment is self-executed. Public WHO and CDC documents supply external
engineering context, but no external person or organization participates in,
reviews, endorses, or approves the work.

## Product boundary

SVEMS comprises an external equipment-monitoring device, a remote data service,
a browser user interface, and notification delivery. It obtains data from a
compatible vaccine refrigerator's logger through a machine-to-machine
interface. The refrigerator, logger, communications networks, user devices, and
public-health programme systems are outside the product boundary.

SVEMS monitors, records, displays, and communicates equipment data. It does not
control refrigeration, determine vaccine viability, or replace programme
procedures for responding to temperature excursions.

## Evidence sequence

1. [`source-register.md`](source-register.md) identifies the authoritative and
   informative inputs.
2. [`system-context.md`](system-context.md) records the product definition,
   stakeholders, operational contexts, assumptions, and exclusions.
3. [`protocol.md`](protocol.md) fixes the trial procedure and evaluation
   questions before requirements are authored.
4. The tag `experiment-0024-baseline-a` identifies the first reviewed source
   set and companion planning artifacts.
5. `change/` contains the controlled change request, impact analysis, focused
   diff, and review record.
6. `product/` contains the advancing work product; the tag
   `experiment-0024-baseline-b` identifies its second reviewed baseline.
7. [`assessment.md`](assessment.md) contains the final trial assessment.

Git commits and annotated experiment tags identify the two requirement
baselines. The current branch does not preserve duplicate baseline directories;
ordinary Git reconstructs each tagged source and companion snapshot. Directory
names are convenient working labels, not requirement identity or
source-language semantics.

## Claims deliberately not made

- The requirements are not a complete product specification.
- The product has not been designed, built, tested, qualified, or certified.
- The work does not demonstrate independent-human authoring or review.
- The provisional companion formats are not mundane-req standards.
