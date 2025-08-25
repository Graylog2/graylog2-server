/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React, { useMemo } from 'react';
import styled, { css } from 'styled-components';

import type { ClickPoint } from 'views/components/visualizations/hooks/usePlotOnClickPopover';
import Value from 'views/components/Value';
import Popover from 'components/common/Popover';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { keySeparator } from 'views/Constants';

const atIndex = (v: string | string[] | undefined, i: number | undefined): T | undefined => {
  if (v == null) return undefined;
  if (Array.isArray(v)) {
    if (typeof i !== 'number') return undefined;

    return (v as T[])[i];
  }

  return v as T;
};
/** Best-effort “hover swatch” color for common traces (bar/scatter/pie). */
export function getHoverSwatchColor(pt: ClickPoint): string | undefined {
  if (!pt) return null;
  const fd = pt.data;

  const idx = pt.pointIndex ?? pt.pointNumber;

  // BAR / SCATTER MARKERS
  const markerPoint = atIndex(fd?.marker?.color, idx) ?? atIndex(fd?.marker?.line?.color, idx);
  if (markerPoint) return markerPoint;

  // PIE-like (colors array)
  const pieColor = atIndex(fd?.marker?.colors, idx) ?? atIndex(fd?.marker?.line?.color, idx);
  if (pieColor) return pieColor;

  // SCATTER LINES (no markers)
  const linePoint = atIndex(fd?.line?.color, idx) ?? fd?.fillcolor;
  if (linePoint) return linePoint;

  // Fallback: single-value marker/line colors (already handled above if arrays)
  return (fd?.marker && (fd.marker.color as string)) || (fd?.line && (fd.line.color as string)) || undefined;
}

const ValueBox = styled.span<{ $bgColor: string }>(
  ({ theme, $bgColor }) => css`
    background-color: ${$bgColor};
    color: ${theme.utils.contrastingColor($bgColor)};
    padding: ${theme.spacings.xxs};
  `,
);

const Container = styled.span(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
    font-size: ${theme.fonts.size.tiny};
  `,
);

const DivContainer = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xxs};
  `,
);
type ValueToRender = { value: number | string; text: string; field: string };
type ValuesToRender = {
  rowPivotValues: Array<ValueToRender>;
  columnPivotValues: Array<ValueToRender>;
  metricValue: ValueToRender;
};

const CartesianOnClickPopoverDropdown = ({
  clickPoint,
  config,
}: {
  clickPoint: ClickPoint;
  config: AggregationWidgetConfig;
}) => {
  const traceColor = getHoverSwatchColor(clickPoint);
  const { rowPivotValues, columnPivotValues, metricValue } = useMemo<ValuesToRender>(() => {
    if (!clickPoint || !config) return {};
    const splitNames = (clickPoint.data.originalName ?? clickPoint.data.name).split(keySeparator);
    const metric = splitNames.pop();

    const columnPivotsToFields = config?.columnPivots?.flatMap((pivot) => pivot.fields) ?? [];

    const rowPivotsToFields = config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [];
    const splitXValues = `${String(clickPoint.x)}`.split(keySeparator);

    return {
      rowPivotValues: splitXValues.map((value, i) => ({ value, field: rowPivotsToFields[i], text: value })),
      columnPivotValues: splitNames.map((value, i) => ({ value, field: columnPivotsToFields[i], text: value })),
      metricValue: { value: clickPoint.y, field: metric, text: `${String(clickPoint.text ?? clickPoint.y)}` },
    };
  }, [clickPoint, config]);

  console.log({ rowPivotValues, columnPivotValues, metricValue });

  return (
    <Popover.Dropdown>
      <DivContainer>
        {metricValue && (
          <Value
            field={metricValue.field}
            value={metricValue.value}
            render={() => (
              <Container>
                <ValueBox $bgColor={traceColor}>{metricValue.text}</ValueBox>
                <span>{metricValue.field}</span>
              </Container>
            )}
          />
        )}
        {!!rowPivotValues?.length && (
          <>
            {rowPivotValues?.map(({ text, value, field }) => (
              <Value
                key={`${value}-${field}`}
                field={field}
                value={value}
                render={() => (
                  <Container>
                    <ValueBox $bgColor={traceColor}>{text}</ValueBox>
                    <span>{field}</span>
                  </Container>
                )}
              />
            ))}
          </>
        )}
        {!!columnPivotValues?.length && (
          <>
            {columnPivotValues?.map(({ text, value, field }) => (
              <Value
                field={field}
                value={value}
                key={`${value}-${field}`}
                render={() => (
                  <Container>
                    <ValueBox $bgColor={traceColor}>{text}</ValueBox>
                    <span>{field}</span>
                  </Container>
                )}
              />
            ))}
          </>
        )}
      </DivContainer>
    </Popover.Dropdown>
  );
};

export default CartesianOnClickPopoverDropdown;
