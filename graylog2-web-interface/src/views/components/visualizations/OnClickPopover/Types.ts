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

import type { PlotData, PlotMouseEvent, Datum, Color } from 'plotly.js';
import type React from 'react';

import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

export type ValueGroupItem = { field: string; value: Datum; text: string; traceColor: string | number };

export type ExtraPlotData = {
  fullData: PlotData;
  data: {
    originalName: string;
    originalLabels: Array<string>;
    marker: {
      colors: Color;
    };
  };
  value: Datum;
  percent: number;
  z: Datum;
};
export type ClickPoint = PlotMouseEvent['points'][number] & ExtraPlotData;
export type Rel = { x: number; y: number };
export type FieldData = {
  field: string;
  value: Datum;
  contexts: { valuePath: Array<{ [key: string]: Datum }> } | null;
};

export type OnClickPopoverDropdownProps = {
  clickPoint: ClickPoint;
  config: AggregationWidgetConfig;
  setFieldData: React.Dispatch<React.SetStateAction<FieldData>>;
};

export type OnClickPopoverDropdown = React.ComponentType<OnClickPopoverDropdownProps>;

export type ValueGroups = {
  metricValue?: ValueGroupItem;
  rowPivotValues?: Array<ValueGroupItem>;
  columnPivotValues?: Array<ValueGroupItem>;
};

export type ValueGroupsProps = ValueGroups & { setFieldData: React.Dispatch<React.SetStateAction<FieldData>> };
