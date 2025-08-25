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

const atIndex = (v: string | string[] | undefined, i: number | undefined): string => {
  if (v == null) return undefined;
  if (Array.isArray(v)) {
    if (typeof i !== 'number') return undefined;

    return v[i];
  }

  return v;
};
/** Best-effort “hover swatch” color for common traces (bar/scatter/pie). */
export function getHoverSwatchColor(pt: ClickPoint): string | undefined {
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

const PieOnClickPopover = ({ clickPoint, config }: { clickPoint: ClickPoint; config: AggregationWidgetConfig }) => {
  if (!clickPoint) return null;

  const traceColor = getHoverSwatchColor(clickPoint);

  const { v: value, pointNumber, data } = clickPoint;
  const valueText = data?.text?.[pointNumber];

  const { rowPivotValues, columnPivotValues, metricValue } = useMemo<ValuesToRender>(() => {
    if (!clickPoint || !config) return {};
    const splitNames = (clickPoint.data.originalName ?? clickPoint.data.name).split(keySeparator);
    // const splitNames = clickPoint.data.originalLabels?.[clickPoint.pointNumber].split(keySeparator);
    const metric = splitNames.pop();

    const columnPivotsToFields = config?.columnPivots?.flatMap((pivot) => pivot.fields) ?? [];

    const rowPivotsToFields = config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [];
    const splitXValues = String(clickPoint.data.originalLabels?.[clickPoint.pointNumber]).split(keySeparator);

    return {
      rowPivotValues: splitXValues.map((value, i) => ({ value, field: rowPivotsToFields[i], text: value })),
      columnPivotValues: splitNames.map((value, i) => ({ value, field: columnPivotsToFields[i], text: value })),
      metricValue: { value: clickPoint.y, field: metric, text: `${String(clickPoint.text ?? clickPoint.y)}` },
    };
  }, [clickPoint, config]);

  console.log({ clickPoint, rowPivotValues, columnPivotValues, metricValue });

  return (
    <Popover.Dropdown title={String(clickPoint?.label)}>
      <Value
        field={data.name}
        value={value}
        render={() => (
          <Container>
            <ValueBox $bgColor={traceColor}>{`${String(valueText ?? value)}`}</ValueBox>
            <span>{clickPoint.data.name}</span>
          </Container>
        )}
      />
    </Popover.Dropdown>
  );
};

export default PieOnClickPopover;
