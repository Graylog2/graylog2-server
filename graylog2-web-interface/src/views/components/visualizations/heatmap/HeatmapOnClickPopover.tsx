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

import Popover from 'components/common/Popover';
import type { ClickPoint, ValueGroups } from 'views/components/visualizations/OnClickPopover/Types';
import { keySeparator } from 'views/Constants';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import OnClickPopoverValueGroups from 'views/components/visualizations/OnClickPopover/OnClickPopoverValueGroups';

const HeatmapOnClickPopover = ({ clickPoint, config }: { config: AggregationWidgetConfig; clickPoint: ClickPoint }) => {
  const { rowPivotValues, columnPivotValues, metricValue } = useMemo<ValueGroups>(() => {
    if (!clickPoint || !config) return {};
    const splitXValues: Array<string | number> = (clickPoint.x as string).split(keySeparator);
    const traceColor = null;
    const metric: string = splitXValues.pop() as string;

    const columnPivotsToFields = config?.columnPivots?.flatMap((pivot) => pivot.fields) ?? [];

    const rowPivotsToFields = config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [];
    const splitYValues: Array<string | number> = `${String(clickPoint.y)}`.split(keySeparator);

    return {
      rowPivotValues: splitYValues.map((value, i) => ({
        value,
        field: rowPivotsToFields[i],
        text: String(value),
        traceColor,
      })),
      columnPivotValues: splitXValues.map((value, i) => ({
        value,
        field: columnPivotsToFields[i],
        text: String(value),
        traceColor,
      })),
      metricValue: {
        value: clickPoint.z,
        field: metric,
        text: String(clickPoint.z),
        traceColor,
      },
    };
  }, [clickPoint, config]);

  return (
    clickPoint && (
      <Popover.Dropdown title={String(clickPoint?.z)}>
        <OnClickPopoverValueGroups
          columnPivotValues={columnPivotValues}
          metricValue={metricValue}
          rowPivotValues={rowPivotValues}
        />
      </Popover.Dropdown>
    )
  );
};

export default HeatmapOnClickPopover;
