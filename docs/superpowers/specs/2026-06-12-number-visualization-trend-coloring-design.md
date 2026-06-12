# NumberVisualization: number lower-right, whole-widget trend coloring

**Date:** 2026-06-12
**Scope:** `graylog2-web-interface/src/views/components/visualizations/number/`

## Goal

Change the `NumberVisualization` widget so that:

1. The metric number is rendered in the **lower-right corner** of the visualization
   (font still auto-sized), in all cases — with and without trend enabled.
2. When trend is enabled, the **whole visualization container** is colored according
   to the trend direction (good/bad/neutral derived from the configured trend
   preference), instead of only the bottom trend bar.
3. The trend details (icon + absolute/relative difference) remain in a bar at the
   very bottom, right-aligned as today, but without their own background — they
   inherit the container's colors.

## Current state

- `NumberVisualization.tsx` renders a CSS grid: a `NumberBox` (1fr, number centered
  via `AutoFontSizer` with `center`) and, when trend is enabled, a `TrendBox` (auto)
  rendering `Trend`.
- `Trend.tsx` computes the difference (unit-converted when a field unit is defined),
  derives the trend direction, and renders a `Background` styled div carrying the
  good/bad/neutral background color plus contrasting text color with `!important`
  and `color-adjust: exact` rules required for report generation.

## Design

### Approach

Lift the trend-direction computation into `NumberVisualization`. The direction only
depends on the sign of `current - previous`, and unit conversion is a positive
linear scaling, so computing it from raw values in the parent is equivalent to
today's unit-converted computation.

Alternatives rejected:

- *Callback from `Trend` to the parent*: inverted data flow, risks render loops.
- *Computing the direction in both components*: duplicated logic that can drift.

### Changes

- **New `trendDirection.ts`** (same directory): exports
  `trendDirection(current, previous, trendPreference): TrendDirection` and the
  `diff` helper, moved out of `Trend.tsx`. Also exports the `TrendDirection` type
  and the theme-color mapping currently in `background()`.
- **`NumberVisualization.tsx`**:
  - Computes the trend direction when `visualizationConfig.trend` is enabled.
  - The grid container gets the trend background color (success/danger for
    good/bad, content background for neutral), contrasting text color, and the
    `!important`/`color-adjust: exact` report-generation rules. It carries
    `data-testid="trend-background"` so color assertions have a target.
  - The number is rendered bottom-right aligned via `AutoFontSizer`.
- **`Trend.tsx`**: loses the `Background` wrapper, the `background()` helper, and
  all per-element color overrides (`TextContainer`, `StyledIcon` color logic).
  Keeps the difference formatting/unit conversion and the icon choice. Text stays
  right-aligned at the bottom.
- **`AutoFontSizer.tsx`**: the `center?: boolean` prop is replaced with
  `alignment?: 'center' | 'bottom-right'` (default: no flex alignment, as today
  without `center`). Both call sites are updated: `NumberVisualization` uses
  `bottom-right`, `EventsNumber` uses `center`.

### Testing

- Move the background-color test cases from `Trend.test.tsx` to
  `NumberVisualization.test.tsx`, asserting the container color for
  good/bad/neutral combinations of values and trend preference.
- Keep the delta-formatting and icon tests in `Trend.test.tsx`.
- `AutoFontSizer.test.tsx`: adapt for the `alignment` prop if it exercises
  `center`.

### Error handling

No new error paths. The existing `N/A` rendering for missing field/value and the
`-- / --` rendering for non-finite differences remain unchanged. When `previous`
is missing or non-finite, the difference is `NaN`, `trendDirection` must return
`neutral` (NaN comparisons are false, so the existing switch falls through to the
delta sign checks — the implementation must ensure NaN yields `neutral`).
