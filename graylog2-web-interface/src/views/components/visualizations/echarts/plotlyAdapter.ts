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
import type { Layout, Shape } from 'views/components/visualizations/types';

type EChartsOption = Record<string, any>;

/**
 * Maps Plotly interpolation shapes to ECharts line step values.
 * Plotly uses: 'linear', 'spline', 'hv', 'vh', 'hvh', 'vhv'
 * ECharts uses: false (linear), 'start', 'middle', 'end', or smooth: true
 */
const mapLineShape = (shape?: string): { step?: string | false; smooth?: boolean } => {
  switch (shape) {
    case 'spline':
      return { smooth: true };
    case 'hv':
      return { step: 'end' };
    case 'vh':
      return { step: 'start' };
    case 'hvh':
      return { step: 'middle' };
    default:
      return {};
  }
};

const mapAxisType = (type?: string): 'value' | 'category' | 'time' | 'log' => {
  switch (type) {
    case 'date':
      return 'time';
    case 'log':
      return 'log';
    case 'category':
      return 'category';
    case 'linear':
    default:
      return 'value';
  }
};

const convertShapesToMarkLines = (shapes: Array<Partial<Shape>> = []) =>
  shapes
    .filter((s) => s.type === 'line' && s.y0 === s.y1)
    .map((s) => ({
      name: s.name ?? '',
      yAxis: s.y0,
      lineStyle: {
        color: s.line?.color ?? '#999',
        type: 'dashed' as const,
      },
      label: {
        show: false,
      },
    }));

const convertTrace = (trace: any, _index: number): any => {
  const { type, name, x, y, z, mode, fill, line, marker, opacity, hole, labels, values, domain,
    hovertemplate, text, customdata } = trace;

  if (type === 'pie') {
    const data = (labels ?? []).map((label: string, i: number) => {
      const val = values?.[i] ?? (y ? y[i] : 0);
      const itemColor = marker?.colors?.[i];

      return {
        name: label,
        value: val,
        ...(itemColor ? { itemStyle: { color: itemColor } } : {}),
      };
    });

    return {
      type: 'pie',
      name,
      data,
      radius: hole ? [`${Math.round(hole * 100)}%`, '70%'] : ['0%', '70%'],
      center: domain
        ? [
            `${Math.round(((domain.x[0] + domain.x[1]) / 2) * 100)}%`,
            `${Math.round(((domain.y[0] + domain.y[1]) / 2) * 100)}%`,
          ]
        : ['50%', '50%'],
      label: {
        show: true,
        position: 'inside',
        formatter: '{d}%',
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)',
        },
      },
      itemStyle: {
        opacity: opacity ?? 1,
      },
    };
  }

  if (type === 'heatmap') {
    const echartsData: Array<[number, number, any]> = [];

    (z ?? []).forEach((row: any[], yi: number) => {
      row.forEach((val, xi) => {
        if (val !== 'None' && val != null) {
          echartsData.push([xi, yi, val]);
        }
      });
    });

    return {
      type: 'heatmap',
      name,
      data: echartsData,
      label: { show: false },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.5)' } },
      ...(customdata
        ? {
            tooltip: {
              formatter: (params: any) => {
                const [xi, yi, val] = params.data;
                const xLabel = x?.[xi] ?? xi;
                const yLabel = y?.[yi] ?? yi;
                const seriesTitle = text?.[yi]?.[xi] ?? '';

                return `${yLabel}<br/>${xLabel}<br/>${seriesTitle}: ${val}`;
              },
            },
          }
        : {}),
    };
  }

  // Scatter / Line / Bar
  const isScatterMarkers = type === 'scatter' && mode === 'markers';
  const isArea = type === 'scatter' && fill === 'tozeroy';
  const isBar = type === 'bar';

  const echartsType = isBar ? 'bar' : isScatterMarkers ? 'scatter' : 'line';

  const series: any = {
    type: echartsType,
    name,
    data: (y ?? []).map((val: any, i: number) => [x?.[i], val]),
    ...(isArea ? { areaStyle: { opacity: 0.3 } } : {}),
    ...mapLineShape(line?.shape),
    ...(opacity != null ? { itemStyle: { opacity } } : {}),
  };

  if (isScatterMarkers) {
    const markerSize = marker?.size ?? 5;
    series.symbolSize = markerSize;
  }

  if (marker?.color && !Array.isArray(marker.color)) {
    series.itemStyle = { ...series.itemStyle, color: marker.color };
    series.lineStyle = { color: marker.color };
  }

  if (line?.color) {
    series.lineStyle = { color: line.color };
    series.itemStyle = { ...series.itemStyle, color: line.color };
  }

  if (text && hovertemplate) {
    series.tooltip = {
      formatter: (params: any) => {
        const idx = params.dataIndex;

        return text[idx] ?? `${params.seriesName}: ${params.value?.[1]}`;
      },
    };
  }

  // Map yaxis assignment (e.g., 'y2' -> yAxisIndex: 1)
  if (trace.yaxis && trace.yaxis !== 'y') {
    const axisNum = parseInt(trace.yaxis.replace('y', ''), 10);
    series.yAxisIndex = axisNum - 1;
  }

  return series;
};

const buildXAxis = (layout: Partial<Layout>, traces: any[], isPie: boolean, isHeatmap: boolean): any => {
  if (isPie) return undefined;

  const xLayout = layout.xaxis ?? {};

  if (isHeatmap) {
    const firstHeatmap = traces.find((t) => t.type === 'heatmap');

    return {
      type: 'category',
      data: firstHeatmap?.x ?? [],
      axisLabel: {
        rotate: 0,
        ...(xLayout.tickfont ? { fontSize: xLayout.tickfont.size, color: xLayout.tickfont.color } : {}),
      },
    };
  }

  const xAxis: any = {
    type: xLayout.type ? mapAxisType(xLayout.type) : 'category',
    axisLabel: {
      ...(xLayout.tickfont ? { fontSize: xLayout.tickfont.size, color: xLayout.tickfont.color } : {}),
    },
  };

  if (xLayout.range) {
    xAxis.min = xLayout.range[0];
    xAxis.max = xLayout.range[1];
  }

  if (xLayout.tickvals && xLayout.ticktext) {
    xAxis.type = 'category';
    xAxis.data = xLayout.ticktext;
  } else if (xLayout.type !== 'date') {
    // Extract x values from traces for category axis
    const allX = traces.flatMap((t) => t.x ?? []);
    const uniqueX = [...new Set(allX)];

    if (uniqueX.length > 0 && xAxis.type === 'category') {
      xAxis.data = uniqueX;
    }
  }

  if (xLayout.title?.text) {
    xAxis.name = xLayout.title.text;
    xAxis.nameLocation = 'middle';
    xAxis.nameGap = 30;
  }

  return xAxis;
};

const buildYAxes = (layout: Partial<Layout>, isPie: boolean, isHeatmap: boolean): any => {
  if (isPie) return undefined;

  if (isHeatmap) {
    const yLayout = layout.yaxis ?? {};

    return {
      type: 'category',
      data: [],
      axisLabel: {
        ...(yLayout.tickfont ? { fontSize: yLayout.tickfont.size, color: yLayout.tickfont.color } : {}),
      },
    };
  }

  // Collect all yaxis definitions from layout (yaxis, yaxis2, yaxis3, ...)
  const axes: any[] = [];
  const yLayout = layout.yaxis ?? {};

  axes.push({
    type: yLayout.type === 'log' ? 'log' : 'value',
    min: yLayout.rangemode === 'tozero' ? 0 : undefined,
    axisLabel: {
      formatter: yLayout.tickformat === ',~r' ? '{value}' : undefined,
      ...(yLayout.tickfont ? { fontSize: yLayout.tickfont.size, color: yLayout.tickfont.color } : {}),
    },
    ...(yLayout.gridcolor ? { splitLine: { lineStyle: { color: yLayout.gridcolor } } } : {}),
    ...(yLayout.title?.text
      ? {
          name: yLayout.title.text,
          nameLocation: 'middle',
          nameGap: 50,
          nameTextStyle: yLayout.title.font ? { color: yLayout.title.font.color } : {},
        }
      : {}),
    ...(yLayout.tickvals
      ? {
          type: 'category',
          data: yLayout.ticktext ?? yLayout.tickvals,
        }
      : {}),
  });

  // Additional y-axes
  let i = 2;

  while (layout[`yaxis${i}`]) {
    const extra = layout[`yaxis${i}`];
    axes.push({
      type: extra.type === 'log' ? 'log' : 'value',
      position: extra.side ?? (i % 2 === 0 ? 'right' : 'left'),
      min: extra.rangemode === 'tozero' ? 0 : undefined,
      offset: i > 2 ? (i - 2) * 60 : 0,
      axisLabel: {
        ...(extra.tickfont ? { fontSize: extra.tickfont.size, color: extra.tickfont.color } : {}),
        ...(extra.tickformat === ',~r' ? { formatter: '{value}' } : {}),
      },
      ...(extra.gridcolor ? { splitLine: { lineStyle: { color: extra.gridcolor } } } : {}),
      ...(extra.title?.text
        ? {
            name: extra.title.text,
            nameLocation: 'middle',
            nameGap: 50,
            nameTextStyle: extra.title.font ? { color: extra.title.font.color } : {},
          }
        : {}),
      ...(extra.tickvals
        ? { type: 'category', data: extra.ticktext ?? extra.tickvals }
        : {}),
    });
    i += 1;
  }

  return axes.length === 1 ? axes[0] : axes;
};

const buildAnnotations = (layout: Partial<Layout>): any[] => {
  const annotations = layout.annotations ?? [];

  return annotations.map((a) => ({
    type: 'text',
    left: typeof a.x === 'number' ? `${a.x * 100}%` : a.x,
    top: 'center',
    style: {
      text: a.text ?? '',
      fill: a.font?.color ?? '#333',
      fontSize: a.font?.size ?? 12,
      backgroundColor: a.bgcolor,
    },
    z: 100,
  }));
};

/**
 * Main adapter: converts Plotly-style traces + layout into an ECharts option.
 */
const plotlyToECharts = (traces: any[], layout: Partial<Layout>): EChartsOption => {
  const isPie = traces.some((t) => t.type === 'pie');
  const isHeatmap = traces.some((t) => t.type === 'heatmap');
  const series = traces.map((trace, i) => convertTrace(trace, i));

  const option: EChartsOption = {
    animation: false,
    series,
    tooltip: {
      trigger: isPie ? 'item' : 'axis',
      confine: true,
    },
    legend: { show: false },
  };

  const xAxis = buildXAxis(layout, traces, isPie, isHeatmap);

  if (xAxis) {
    option.xAxis = xAxis;
  }

  const yAxis = buildYAxes(layout, isPie, isHeatmap);

  if (yAxis) {
    option.yAxis = yAxis;

    // Set heatmap y-axis data from first heatmap trace
    if (isHeatmap) {
      const firstHeatmap = traces.find((t) => t.type === 'heatmap');

      if (firstHeatmap?.y && typeof yAxis === 'object' && !Array.isArray(yAxis)) {
        yAxis.data = firstHeatmap.y;
      }
    }
  }

  if (!isPie) {
    option.grid = {
      left: layout.margin?.l ?? 40,
      right: layout.margin?.r ?? 10,
      top: layout.margin?.t ?? 10,
      bottom: layout.margin?.b ?? 0,
      containLabel: true,
    };
  }

  // Convert shapes to markLines on the first applicable series
  const markLineData = convertShapesToMarkLines(layout.shapes as Array<Partial<Shape>>);

  if (markLineData.length > 0 && series.length > 0) {
    const targetSeries = series.find((s: any) => s.type !== 'pie') ?? series[0];
    targetSeries.markLine = {
      silent: true,
      symbol: 'none',
      data: markLineData,
    };
  }

  // Annotations as graphic elements
  const graphicElements = buildAnnotations(layout);

  if (graphicElements.length > 0) {
    option.graphic = graphicElements;
  }

  // Heatmap visual map
  if (isHeatmap) {
    const firstHeatmap = traces.find((t) => t.type === 'heatmap');
    option.visualMap = {
      min: firstHeatmap?.zmin ?? 0,
      max: firstHeatmap?.zmax ?? 100,
      calculable: true,
      orient: 'vertical',
      right: 0,
      top: 'center',
    };
  }

  // Bar mode
  if (layout.barmode === 'stack' || layout.barmode === 'relative') {
    option.series = series.map((s: any) =>
      s.type === 'bar' ? { ...s, stack: 'total' } : s,
    );
  }

  return option;
};

export default plotlyToECharts;
