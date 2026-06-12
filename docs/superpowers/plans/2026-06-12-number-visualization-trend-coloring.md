# NumberVisualization Trend Coloring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render the number of the `NumberVisualization` widget in the lower-right corner (auto-sized font) and color the whole visualization container according to the trend direction, instead of only the bottom trend bar.

**Architecture:** The trend-direction computation (`good`/`bad`/`neutral` from current value, previous value, and trend preference) moves out of `Trend.tsx` into a new shared module `trendDirection.ts`. `NumberVisualization` computes the direction and colors its grid container; `Trend` becomes a colorless text/icon row that inherits the container's colors. `AutoFontSizer` gets an `alignment` prop (`'center' | 'bottom-right'`) replacing the `center` boolean.

**Tech Stack:** React, TypeScript, styled-components, Jest + Testing Library (`wrappedTestingLibrary`), jest-styled-components (`toHaveStyleRule`).

**Spec:** `docs/superpowers/specs/2026-06-12-number-visualization-trend-coloring-design.md`

---

## Important context for the executor

- All frontend work happens in `graylog2-web-interface/`. Run all `yarn` commands from that directory.
- Read `graylog2-web-interface/AGENTS.md` and `graylog2-web-interface/CONTRIBUTING.md` before starting.
- Every new source file needs the SSPL license header (copy the exact header block from `graylog2-web-interface/src/views/components/visualizations/number/Trend.tsx`, lines 1–16).
- Commit messages in this repo are plain sentence-case (no `feat:`/`fix:` prefixes), e.g. "Show sidecar API token hint regardless of configured sidecar_user name". End commit messages with the `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer.
- Behavior change that is intentional (documented in the spec): a non-finite delta (missing/NaN previous value) now always yields a `neutral` direction. The old code returned `good` for `NaN` with `LOWER` preference by accident.

### File structure

| File | Action | Responsibility |
| --- | --- | --- |
| `src/views/components/visualizations/number/trendDirection.ts` | Create | Trend direction + diff computation, trend background color mapping |
| `src/views/components/visualizations/number/trendDirection.test.ts` | Create | Unit tests for direction logic incl. NaN handling |
| `src/views/components/visualizations/number/AutoFontSizer.tsx` | Modify | `alignment` prop instead of `center` |
| `src/views/components/widgets/events/EventsNumber.tsx` | Modify | Call site: `center` → `alignment="center"` |
| `src/views/components/visualizations/number/NumberVisualization.tsx` | Modify | Container coloring, number bottom-right |
| `src/views/components/visualizations/number/NumberVisualization.test.tsx` | Modify | Integration tests for container coloring |
| `src/views/components/visualizations/number/Trend.tsx` | Modify | Remove background/colors, drop `trendPreference` prop |
| `src/views/components/visualizations/number/Trend.test.tsx` | Modify | Remove background tests, drop `trendPreference` usage |

All relative paths below are relative to `graylog2-web-interface/`.

---

### Task 1: `trendDirection` module

**Files:**
- Create: `src/views/components/visualizations/number/trendDirection.ts`
- Test: `src/views/components/visualizations/number/trendDirection.test.ts`

- [ ] **Step 1: Write the failing test**

Create `src/views/components/visualizations/number/trendDirection.test.ts` (prepend the SSPL license header copied from `Trend.tsx`):

```ts
import trendDirection from './trendDirection';

describe('trendDirection', () => {
  it.each`
    current | previous | preference   | expected
    ${42}   | ${42}    | ${'NEUTRAL'} | ${'neutral'}
    ${42}   | ${42}    | ${'HIGHER'}  | ${'neutral'}
    ${42}   | ${42}    | ${'LOWER'}   | ${'neutral'}
    ${43}   | ${42}    | ${'HIGHER'}  | ${'good'}
    ${41}   | ${42}    | ${'HIGHER'}  | ${'bad'}
    ${43}   | ${42}    | ${'LOWER'}   | ${'bad'}
    ${41}   | ${42}    | ${'LOWER'}   | ${'good'}
    ${43}   | ${42}    | ${'NEUTRAL'} | ${'neutral'}
    ${41}   | ${42}    | ${'NEUTRAL'} | ${'neutral'}
  `(
    'returns $expected for current=$current, previous=$previous, preference=$preference',
    ({ current, previous, preference, expected }) => {
      expect(trendDirection(current, previous, preference)).toBe(expected);
    },
  );

  it('returns neutral when previous value is missing', () => {
    expect(trendDirection(42, undefined, 'LOWER')).toBe('neutral');
    expect(trendDirection(42, null, 'HIGHER')).toBe('neutral');
  });

  it('returns neutral when previous value is NaN', () => {
    expect(trendDirection(42, NaN, 'LOWER')).toBe('neutral');
    expect(trendDirection(42, NaN, 'HIGHER')).toBe('neutral');
  });

  it('returns neutral when current value is missing', () => {
    expect(trendDirection(undefined, 42, 'LOWER')).toBe('neutral');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `yarn test --testPathPattern=visualizations/number/trendDirection`
Expected: FAIL — cannot find module `./trendDirection`.

- [ ] **Step 3: Write the implementation**

Create `src/views/components/visualizations/number/trendDirection.ts` (prepend the SSPL license header):

```ts
import type { DefaultTheme } from 'styled-components';

import type { TrendPreference } from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';

export type TrendDirection = 'good' | 'bad' | 'neutral';

export const trendBackground = (theme: DefaultTheme, trend: TrendDirection = 'neutral') =>
  ({
    good: theme.colors.variant.success,
    bad: theme.colors.variant.danger,
    neutral: theme.colors.global.contentBackground,
  })[trend];

export const diff = (current: number | undefined, previous: number | undefined): [number, number] => {
  if (typeof current === 'number' && typeof previous === 'number') {
    const difference = current - previous;
    const differencePercent = difference / previous;

    return [difference, differencePercent];
  }

  return [NaN, NaN];
};

const trendDirection = (
  current: number | undefined,
  previous: number | undefined | null,
  trendPreference: TrendPreference,
): TrendDirection => {
  const [delta] = diff(current, previous ?? undefined);

  if (!Number.isFinite(delta) || delta === 0) {
    return 'neutral';
  }

  switch (trendPreference) {
    case 'LOWER':
      return delta > 0 ? 'bad' : 'good';
    case 'HIGHER':
      return delta > 0 ? 'good' : 'bad';
    case 'NEUTRAL':
    default:
      return 'neutral';
  }
};

export default trendDirection;
```

Notes: `diff` is a verbatim move from `Trend.tsx` (it gets deleted there in Task 4). `trendBackground` is the moved `background()` helper from `Trend.tsx`. The `!Number.isFinite(delta)` guard is the intentional NaN fix from the spec.

- [ ] **Step 4: Run test to verify it passes**

Run: `yarn test --testPathPattern=visualizations/number/trendDirection`
Expected: PASS (12 tests).

- [ ] **Step 5: Lint and commit**

```bash
yarn lint:path src/views/components/visualizations/number/trendDirection.ts src/views/components/visualizations/number/trendDirection.test.ts
git add src/views/components/visualizations/number/trendDirection.ts src/views/components/visualizations/number/trendDirection.test.ts
git commit -m "Extract trend direction computation into shared module

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2: `AutoFontSizer` alignment prop

**Files:**
- Modify: `src/views/components/visualizations/number/AutoFontSizer.tsx`
- Modify: `src/views/components/widgets/events/EventsNumber.tsx:44`

No new tests: the change is pure CSS positioning (flex alignment); the existing `AutoFontSizer.test.tsx` covers the font-size logic, which is untouched. Asserting flex CSS rules would test styled-components, not behavior.

- [ ] **Step 1: Replace the `center` prop with `alignment`**

In `src/views/components/visualizations/number/AutoFontSizer.tsx`, replace the `FontSize` styled component:

```tsx
type Alignment = 'center' | 'bottom-right';

const FontSize = styled.div<{ fontSize: number; $alignment: Alignment | undefined }>`
  height: 100%;
  width: 100%;
  font-size: ${(props) => css`
    ${props.fontSize}px
  `};
  ${(props) =>
    props.$alignment === 'center'
      ? css`
          display: flex;
          justify-content: center;
          align-items: center;
        `
      : ''}
  ${(props) =>
    props.$alignment === 'bottom-right'
      ? css`
          display: flex;
          justify-content: flex-end;
          align-items: flex-end;
        `
      : ''}
`;
```

Replace `center?: boolean;` in `Props` with `alignment?: Alignment;`, and update the component signature and render:

```tsx
const AutoFontSizer = ({ children, target = null, height, width, alignment = undefined }: Props) => {
  const _container = useRef<HTMLElement | undefined>();
  const fontSize = useAutoFontSize(target, _container, height, width);
  const _mixedContainer: { current } = _container;

  return (
    <FontSize $alignment={alignment} fontSize={fontSize} ref={_mixedContainer}>
      {children}
    </FontSize>
  );
};
```

- [ ] **Step 2: Update the `EventsNumber` call site**

In `src/views/components/widgets/events/EventsNumber.tsx` line 44, change:

```tsx
<AutoFontSizer height={height} width={width} center>
```

to:

```tsx
<AutoFontSizer height={height} width={width} alignment="center">
```

(The `NumberVisualization.tsx` call site still passes `center` at this point — it is updated in Task 3. To keep this task compiling on its own, update it here already by changing `<AutoFontSizer height={height} width={width} center>` in `NumberVisualization.tsx:127` to `<AutoFontSizer height={height} width={width} alignment="center">`; Task 3 changes it to `bottom-right`.)

- [ ] **Step 3: Verify compilation and existing tests**

Run: `yarn tsgo`
Expected: no errors.

Run: `yarn test --testPathPattern=visualizations/number`
Expected: PASS (AutoFontSizer, NumberVisualization, Trend, trendDirection suites all green).

- [ ] **Step 4: Lint and commit**

```bash
yarn lint:path src/views/components/visualizations/number/AutoFontSizer.tsx src/views/components/widgets/events/EventsNumber.tsx src/views/components/visualizations/number/NumberVisualization.tsx
git add -u
git commit -m "Replace AutoFontSizer center prop with alignment prop

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 3: `NumberVisualization` — container coloring and bottom-right number

**Files:**
- Modify: `src/views/components/visualizations/number/NumberVisualization.tsx`
- Test: `src/views/components/visualizations/number/NumberVisualization.test.tsx`

- [ ] **Step 1: Write the failing tests**

In `src/views/components/visualizations/number/NumberVisualization.test.tsx`, add imports:

```tsx
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import type { TrendPreference } from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
```

Inside the `describe('NumberVisualization', ...)` block (after the `SimplifiedNumberVisualization` definition), add helpers:

```tsx
const rowsWithValue = (value: number): Rows => [
  {
    key: [],
    source: 'leaf',
    values: [
      {
        key: ['count()'],
        rollup: true,
        source: 'row-leaf',
        value,
      },
    ],
  },
];

const dataWithTrend = (current: number, previous: number): Data => ({
  chart: rowsWithValue(current),
  trend: rowsWithValue(previous),
});

const configWithTrend = (trendPreference: TrendPreference) =>
  AggregationWidgetConfig.builder()
    .series([Series.forFunction('count()')])
    .visualization('numeric')
    .visualizationConfig(NumberVisualizationConfig.create(true, trendPreference))
    .build();
```

At the end of the `describe` block, add:

```tsx
describe('colors the container according to value and trend preference', () => {
  it.each`
    current | previous | preference   | expectedColor
    ${42}   | ${42}    | ${'HIGHER'}  | ${'#fff'}
    ${43}   | ${42}    | ${'HIGHER'}  | ${'#2ECA8F'}
    ${41}   | ${42}    | ${'HIGHER'}  | ${'#FE4A49'}
    ${41}   | ${42}    | ${'LOWER'}   | ${'#2ECA8F'}
    ${43}   | ${42}    | ${'LOWER'}   | ${'#FE4A49'}
    ${43}   | ${42}    | ${'NEUTRAL'} | ${'#fff'}
  `(
    'shows $expectedColor for current=$current, previous=$previous, preference=$preference',
    async ({ current, previous, preference, expectedColor }) => {
      render(
        <SimplifiedNumberVisualization data={dataWithTrend(current, previous)} config={configWithTrend(preference)} />,
      );

      const container = await screen.findByTestId('trend-background');

      expect(container).toHaveStyleRule('background-color', `${expectedColor}!important`);
    },
  );

  it('does not color the container when trend is disabled', async () => {
    render(<SimplifiedNumberVisualization />);

    const container = await screen.findByTestId('trend-background');

    expect(container).not.toHaveStyleRule('background-color');
  });
});
```

`SUTProps` must allow a `config` override — extend it:

```tsx
type SUTProps = {
  data?: Data;
  config?: AggregationWidgetConfig;
};
```

(`AggregationWidgetConfig` is already imported; if the existing import is type-only adjust accordingly — it is a value import already, used by the builder.)

- [ ] **Step 2: Run tests to verify the new ones fail**

Run: `yarn test --testPathPattern=visualizations/number/NumberVisualization`
Expected: FAIL — `findByTestId('trend-background')` finds the (still existing) Trend bar for trend cases, but the assertions on the container color fail for the disabled case and/or colors. The important thing: the 7 new tests do not all pass yet.

- [ ] **Step 3: Implement container coloring and bottom-right alignment**

In `src/views/components/visualizations/number/NumberVisualization.tsx`:

Add imports:

```tsx
import trendDirection, { trendBackground } from './trendDirection';
import type { TrendDirection } from './trendDirection';
```

Replace the `Container` styled component (lines 38–43) with:

```tsx
const Container = styled.div<{ $height: number; $trend: TrendDirection | undefined }>(({ theme, $height, $trend }) => {
  const bgColor = trendBackground(theme, $trend);

  return css`
    height: ${$height}px;
    width: 100%;
    ${$trend &&
    css`
      background-color: ${bgColor} !important; /* Needed for report generation */
      color: ${theme.utils.contrastingColor(bgColor)} !important; /* Needed for report generation */
      color-adjust: exact !important; /* Needed for report generation */
    `}
  `;
});
```

(`GridContainer` and `SingleItemGrid` extend `Container` and inherit the new prop automatically.)

In the component body, after `visualizationConfig` is defined and `value`/`previousValue` are extracted, compute the direction (place it directly above the `if (!field || ...)` early return):

```tsx
const trend = visualizationConfig.trend
  ? trendDirection(value, previousValue, visualizationConfig.trendPreference)
  : undefined;
```

Update the render: pass the trend and test id to the container, and align the number bottom-right:

```tsx
return (
  <ContainerComponent $height={heightProp} $trend={trend} data-testid="trend-background">
    <NumberBox resizeDelay={20}>
      {({ height, width }) => (
        <AutoFontSizer height={height} width={width} alignment="bottom-right">
          <CustomHighlighting field={field} value={value}>
            <Value field={field} type={fieldTypeFor(field, fields)} value={value} render={DecoratedValue} unit={unit} />
          </CustomHighlighting>
        </AutoFontSizer>
      )}
    </NumberBox>
    {visualizationConfig.trend && (
      <TrendBox>
        {({ height, width }) => (
          <AutoFontSizer height={height} width={width} target={targetRef}>
            <Trend
              ref={targetRef}
              current={value}
              previous={previousValue}
              trendPreference={visualizationConfig.trendPreference}
              unit={unit}
            />
          </AutoFontSizer>
        )}
      </TrendBox>
    )}
  </ContainerComponent>
);
```

(`trendPreference` is still passed to `Trend` here; Task 4 removes it.)

- [ ] **Step 4: Run tests to verify they pass**

Run: `yarn test --testPathPattern=visualizations/number/NumberVisualization`
Expected: PASS. Caveat: the Trend bar still renders its own `data-testid="trend-background"` until Task 4, so `findByTestId` would match two elements and throw. If that happens, this is the expected intermediate state — proceed to rename the test id on the Trend bar as part of this step by removing `data-testid="trend-background"` from the `Background` element in `src/views/components/visualizations/number/Trend.tsx` (line 166) and deleting the now-failing background assertions in `Trend.test.tsx` (the whole `describe('renders background according to values and trend preference', ...)` block, lines 96–160) — those assertions move to `NumberVisualization.test.tsx` which now covers the same matrix. Then re-run both:

Run: `yarn test --testPathPattern=visualizations/number`
Expected: PASS.

- [ ] **Step 5: Lint and commit**

```bash
yarn lint:path src/views/components/visualizations/number/NumberVisualization.tsx src/views/components/visualizations/number/NumberVisualization.test.tsx src/views/components/visualizations/number/Trend.tsx src/views/components/visualizations/number/Trend.test.tsx
git add -u
git commit -m "Color whole number visualization by trend and move number to lower right

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 4: Strip colors from `Trend`

**Files:**
- Modify: `src/views/components/visualizations/number/Trend.tsx`
- Modify: `src/views/components/visualizations/number/Trend.test.tsx`
- Modify: `src/views/components/visualizations/number/NumberVisualization.tsx` (call site)

- [ ] **Step 1: Rewrite `Trend.tsx` without background/color logic**

Replace the full content of `src/views/components/visualizations/number/Trend.tsx` with (keep the existing SSPL license header):

```tsx
import * as React from 'react';
import styled, { css } from 'styled-components';

import Icon from 'components/common/Icon';
import type FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import { getPrettifiedValue, convertValueToUnit } from 'views/components/visualizations/utils/unitConverters';
import formatValueWithUnitLabel from 'views/components/visualizations/utils/formatValueWithUnitLabel';
import getUnitTextLabel from 'views/components/visualizations/utils/getUnitTextLabel';
import { formatTrend } from 'util/NumberFormatting';

import { diff } from './trendDirection';

const TextContainer = styled.div(
  ({ theme }) => css`
    margin: 5px;
    text-align: right;
    font-family: ${theme.fonts.family.body};
  `,
);

const _trendIcon = (delta: number) =>
  // eslint-disable-next-line no-nested-ternary
  delta === 0 ? 'arrow_circle_right' : delta > 0 ? 'arrow_circle_up' : 'arrow_circle_down';

const getTrendConvertedValues = (
  current: number,
  previous: number,
  fieldUNit: FieldUnit,
): {
  previousConverted: number | string;
  differenceConverted: number;
  differencePercent: number;
  unitAbbrevString: string;
} => {
  const [difference, differencePercent] = diff(current, previous);

  if (!fieldUNit?.isDefined) {
    return {
      previousConverted: previous,
      differenceConverted: difference,
      differencePercent,
      unitAbbrevString: '',
    };
  }

  const originalParams = { unitType: fieldUNit?.unitType, abbrev: fieldUNit?.abbrev };
  const { unit: currentPrettyUnit } = getPrettifiedValue(current, originalParams);
  const currentPrettyParams = { unitType: currentPrettyUnit?.unitType, abbrev: currentPrettyUnit?.abbrev };
  const { value: prettyDiff } = convertValueToUnit(difference, originalParams, currentPrettyParams);
  const { value: previousPretty } = convertValueToUnit(previous, originalParams, currentPrettyParams);

  return {
    previousConverted: `${formatValueWithUnitLabel(previousPretty, currentPrettyUnit.abbrev)} (${previous})`,
    unitAbbrevString: ` ${getUnitTextLabel(currentPrettyUnit.abbrev)}`,
    differenceConverted: prettyDiff,
    differencePercent,
  };
};

type Props = {
  current: number;
  previous: number | undefined | null;
  unit?: FieldUnit;
};

const Trend = ({ current, previous, unit = undefined }: Props, ref: React.ForwardedRef<HTMLDivElement>) => {
  const { differenceConverted, differencePercent, unitAbbrevString, previousConverted } = getTrendConvertedValues(
    current,
    previous,
    unit,
  );

  const trendIcon = _trendIcon(differenceConverted);

  const absoluteDifference = Number.isFinite(differenceConverted)
    ? `${formatTrend(differenceConverted)}${unitAbbrevString}`
    : '--';
  const relativeDifference = Number.isFinite(differencePercent)
    ? formatTrend(differencePercent, { percentage: true })
    : '--';

  return (
    <TextContainer ref={ref}>
      <Icon name={trendIcon} data-testid="trend-icon" />{' '}
      <span data-testid="trend-value" title={`Previous value: ${previousConverted}`}>
        {absoluteDifference} / {relativeDifference}
      </span>
    </TextContainer>
  );
};

Trend.displayName = 'Trend';

export default React.forwardRef(Trend);
```

What changed vs. the original: `Background`, `StyledIcon`, `background()`, `_trendDirection`, `diff` (now imported), the `TrendDirection` type, the `TrendPreference`/`DefaultTheme` imports, and the `trendPreference` prop are gone. `text-align: right` moved from `Background` to `TextContainer`. The `ref` moved to `TextContainer`'s root role (same element as before, still the measured target for `AutoFontSizer`).

- [ ] **Step 2: Remove `trendPreference` from the call site**

In `src/views/components/visualizations/number/NumberVisualization.tsx`, remove the `trendPreference={visualizationConfig.trendPreference}` line from the `<Trend ... />` element.

- [ ] **Step 3: Update `Trend.test.tsx`**

In `src/views/components/visualizations/number/Trend.test.tsx`:
- Remove `trendPreference` from the `renderTrend` helper:

```tsx
const renderTrend = ({ current = 42, previous = 42 }: Partial<React.ComponentProps<typeof Trend>> = {}) =>
  render(<Trend current={current} previous={previous} />);
```

- Delete the `describe('renders background according to values and trend preference', ...)` block if it still exists (it may already have been removed in Task 3).
- Keep all delta-formatting tests and the `describe('renders icon indicating trend direction', ...)` block unchanged.

- [ ] **Step 4: Verify compilation and tests**

Run: `yarn tsgo`
Expected: no errors (in particular, no leftover `trendPreference` usages).

Run: `yarn test --testPathPattern=visualizations/number`
Expected: PASS — all four suites.

- [ ] **Step 5: Lint and commit**

```bash
yarn lint:path src/views/components/visualizations/number/Trend.tsx src/views/components/visualizations/number/Trend.test.tsx src/views/components/visualizations/number/NumberVisualization.tsx
git add -u
git commit -m "Remove background coloring from Trend component

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 5: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Type check**

Run (in `graylog2-web-interface/`): `yarn tsgo`
Expected: no errors.

- [ ] **Step 2: Lint changed files**

Run: `yarn lint:changes`
Expected: no errors (requires the commits from Tasks 1–4 to exist).

- [ ] **Step 3: Run the affected test suites**

Run: `yarn test --testPathPattern="visualizations/number|widgets/events"`
Expected: PASS.

- [ ] **Step 4: Run the full test suite**

Run: `yarn test`
Expected: PASS. This is long-running; it is required by `graylog2-web-interface/AGENTS.md` before finishing work.

- [ ] **Step 5: Review against conventions**

Re-read the "Key Technical Decisions" section of `graylog2-web-interface/AGENTS.md` and confirm the diff complies (TypeScript, functional components, no PropTypes, `??` over `||`, no new react-bootstrap imports). No commit needed unless fixes are required.
