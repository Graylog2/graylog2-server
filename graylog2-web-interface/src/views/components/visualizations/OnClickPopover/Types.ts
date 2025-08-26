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
