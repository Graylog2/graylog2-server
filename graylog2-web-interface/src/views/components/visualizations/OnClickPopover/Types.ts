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

export type ValueGroupItem = { field: string; value: Datum; text: string; traceColor: string | number };

export type ValueGroups = {
  metricValue?: ValueGroupItem;
  rowPivotValues?: Array<ValueGroupItem>;
  columnPivotValues?: Array<ValueGroupItem>;
};
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
};
export type ClickPoint = PlotMouseEvent['points'][number] & ExtraPlotData;
export type Pos = { left: number; top: number } | null;
export type Rel = { x: number; y: number };
