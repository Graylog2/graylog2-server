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

/**
 * Chart layout types — these replace the Plotly.js type imports.
 * They define the shape of layout/config objects used throughout
 * the visualization layer. The actual rendering is done by ECharts
 * via the adapter in echarts/plotlyAdapter.ts.
 */

export type Color = string | number;

export type Datum = string | number | Date | null;

export type Font = {
  color?: string;
  size?: number;
  family?: string;
};

export type LayoutAxis = {
  automargin?: boolean;
  gridcolor?: string;
  tickfont?: Font;
  tickformat?: string;
  tickvals?: Array<any>;
  ticktext?: Array<string>;
  title?: { font?: Font; text?: string; automargin?: boolean; standoff?: number };
  type?: 'linear' | 'log' | 'date' | 'category' | '-';
  range?: Array<any>;
  fixedrange?: boolean;
  rangemode?: string;
  autoshift?: boolean;
  autorange?: boolean;
  ticklabelposition?: string;
  ticklabelstandoff?: number;
  side?: string;
  overlaying?: string;
  domain?: Array<number>;
  [key: string]: any;
};

export type Shape = {
  type?: 'line' | 'rect' | 'circle';
  x0?: number | string;
  x1?: number | string;
  y0?: number | string;
  y1?: number | string;
  xref?: string;
  yref?: string;
  name?: string;
  opacity?: number;
  layer?: string;
  line?: { color?: string; width?: number; dash?: string };
};

export type Annotations = {
  x?: number | string;
  y?: number | string;
  xref?: string;
  yref?: string;
  text?: string;
  showarrow?: boolean;
  xanchor?: string;
  yanchor?: string;
  align?: string;
  font?: Font;
  xshift?: number;
  yshift?: number;
  bgcolor?: string;
  bordercolor?: string;
  borderpad?: number;
};

export type Layout = {
  shapes?: Array<Partial<Shape>>;
  annotations?: Array<Partial<Annotations>>;
  autosize?: boolean;
  showlegend?: boolean;
  margin?: { t?: number; l?: number; r?: number; b?: number; pad?: number };
  legend?: { orientation?: string; font?: Font; y?: number };
  hoverlabel?: { namelength?: number };
  hovermode?: string | boolean;
  paper_bgcolor?: string;
  plot_bgcolor?: string;
  title?: { font?: Font };
  yaxis?: Partial<LayoutAxis>;
  xaxis?: Partial<LayoutAxis>;
  barmode?: string;
  [key: string]: any;
};

export type PlotData = {
  type: string;
  name?: string;
  x?: Array<any>;
  y?: Array<any>;
  z?: Array<Array<any>>;
  mode?: string;
  marker?: { color?: Color | Array<Color>; colors?: Color | Array<Color>; size?: number; line?: { color?: Color } };
  line?: { color?: Color; shape?: string };
  fill?: string;
  fillcolor?: string | number;
  text?: Array<string>;
  hovertemplate?: string;
  opacity?: number;
  hole?: number;
  labels?: Array<string>;
  values?: Array<any>;
  domain?: { x?: [number, number]; y?: [number, number] };
  customdata?: any;
  colorscale?: string;
  colorbar?: any;
  reversescale?: boolean;
  zmin?: number;
  zmax?: number;
  uid?: string;
  [key: string]: any;
};

export type PlotDatum = {
  curveNumber: number;
  pointIndex?: number;
  pointNumber?: number;
  x?: any;
  y?: any;
  z?: any;
  value?: any;
  percent?: number;
  label?: string;
  data?: PlotData;
  fullData?: PlotData;
  bbox?: { x0: number; y0: number; x1?: number; y1?: number };
  [key: string]: any;
};

export type PlotMouseEvent = {
  points: Array<PlotDatum>;
  event: MouseEvent;
};

export type EChartsInstance = {
  getZr: () => any;
  getDom: () => HTMLElement;
  resize: () => void;
  dispose: () => void;
  on: (event: string, handler: (...args: any[]) => void) => void;
  off: (event: string, handler?: (...args: any[]) => void) => void;
  convertFromPixel: (finder: any, value: any) => any;
  convertToPixel: (finder: any, value: any) => any;
  getOption: () => any;
};
