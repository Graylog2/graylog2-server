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
import * as React from 'react';
import { useCallback, useMemo, useState, useRef, useLayoutEffect, useEffect } from 'react';
import type { Layout } from 'plotly.js';
import { createPortal } from 'react-dom';

import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import { DateType } from 'views/logic/aggregationbuilder/Pivot';
import BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import useChartData from 'views/components/visualizations/useChartData';
import useEvents from 'views/components/visualizations/useEvents';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import { keySeparator, humanSeparator } from 'views/Constants';
import type { ChartConfig } from 'views/components/visualizations/GenericPlot';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type ColorMapper from 'views/components/visualizations/ColorMapper';
import useChartLayoutSettingsWithCustomUnits from 'views/components/visualizations/hooks/useChartLayoutSettingsWithCustomUnits';
import useBarChartDataSettingsWithCustomUnits from 'views/components/visualizations/hooks/useBarChartDataSettingsWithCustomUnits';
import Popover from 'components/common/Popover';

import type { Generator } from '../ChartData';
import XYPlot from '../XYPlot';

type ChartDefinition = {
  type: string;
  name: string;
  x?: Array<string>;
  y?: Array<any>;
  z?: Array<Array<any>>;
  opacity?: number;
  originalName: string;
  unit?: string;
  yaxis?: string;
};

const setChartColor = (chart: ChartConfig, colors: ColorMapper) => ({
  marker: { color: colors.get(chart.originalName ?? chart.name) },
});

const defineSingleDateBarWidth = (
  chartDataResult: ChartDefinition[],
  config: AggregationWidgetConfig,
  timeRangeFrom: string,
  timeRangeTo: string,
) => {
  const barWidth = 0.03; // width in percentage, relative to chart width
  const minXUnits = 30;

  if (config.rowPivots.length !== 1 || config.rowPivots[0].type !== DateType) {
    return chartDataResult;
  }

  return chartDataResult.map((data) => {
    if (data?.x?.length === 1) {
      // @ts-ignore
      const timeRangeMS = new Date(timeRangeTo) - new Date(timeRangeFrom);
      const widthXUnits = timeRangeMS * barWidth;

      return {
        ...data,
        width: [Math.max(minXUnits, widthXUnits)],
      };
    }

    return data;
  });
};

type AnchorEl = Element | null;

const useAnchorRect = (anchor: AnchorEl) => {
  const [rect, setRect] = useState<DOMRect | null>(null);

  const update = useCallback(() => {
    if (!anchor) return setRect(null);
    const r = anchor.getBoundingClientRect();
    setRect(r);
  }, [anchor]);

  useLayoutEffect(() => {
    update();
    if (!anchor) return;

    // Track layout changes
    const ro = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(() => update()) : null;

    ro?.observe(anchor as Element);
    // Also watch the chart container (parent) if present
    const parent = (anchor as HTMLElement).closest('.js-plotly-plot');
    parent && ro?.observe(parent);

    // On scroll/resize, recompute
    const onWin = () => update();
    window.addEventListener('scroll', onWin, { passive: true });
    window.addEventListener('resize', onWin, { passive: true });

    return () => {
      ro?.disconnect();
      window.removeEventListener('scroll', onWin);
      window.removeEventListener('resize', onWin);
    };
  }, [anchor, update]);

  return {
    left: rect ? rect.left + rect.width / 2 : 0,
    top: rect ? rect.top : 0,
  };
};

type Anchor = Element;

const getBarElement = (graphDiv: HTMLElement, curveNumber: number, pointIndex: number): Element | null => {
  const traces = graphDiv.querySelectorAll('.barlayer .trace');
  const trace = traces[curveNumber] as HTMLElement | undefined;
  if (!trace) return null;

  const points = trace.querySelectorAll('.point');
  const point = points[pointIndex] as HTMLElement | undefined;
  if (!point) return null;

  return point.querySelector('rect') ?? point.querySelector('path');
};

const BarVisualization = makeVisualization(
  ({ config, data, effectiveTimerange, height, width }: VisualizationComponentProps) => {
    const visualizationConfig = (config.visualizationConfig ??
      BarVisualizationConfig.empty()) as BarVisualizationConfig;

    const barmode = useMemo(
      () => (visualizationConfig && visualizationConfig.barmode ? visualizationConfig.barmode : undefined),
      [visualizationConfig],
    );

    const mapKeys = useMapKeys();
    const rowPivotFields = useMemo(
      () => config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [],
      [config?.rowPivots],
    );
    const _mapKeys = useCallback(
      (labels: string[]) =>
        labels.map((label) =>
          label
            .split(keySeparator)
            .map((l, i) => mapKeys(l, rowPivotFields[i]))
            .join(humanSeparator),
        ),
      [mapKeys, rowPivotFields],
    );

    const getBarChartDataSettingsWithCustomUnits = useBarChartDataSettingsWithCustomUnits({
      config,
      barmode,
    });

    const _seriesGenerator: Generator = useCallback(
      ({ type, name, labels, values, originalName, total, idx, fullPath }): ChartDefinition => {
        const opacity = visualizationConfig?.opacity ?? 1.0;
        const mappedKeys = _mapKeys(labels);

        return {
          type,
          name,
          x: mappedKeys,
          y: values,
          opacity,
          originalName,
          ...getBarChartDataSettingsWithCustomUnits({
            originalName,
            name,
            fullPath,
            values,
            idx,
            total,
            xAxisItemsLength: mappedKeys.length,
          }),
        };
      },
      [visualizationConfig?.opacity, _mapKeys, getBarChartDataSettingsWithCustomUnits],
    );

    const rows = useMemo(() => retrieveChartData(data), [data]);

    const _chartDataResult = useChartData(rows, {
      widgetConfig: config,
      chartType: 'bar',
      generator: _seriesGenerator,
    });

    const { eventChartData, shapes } = useEvents(config, data.events);

    // const layout = shapes ? { ..._layout, shapes } : _layout;

    const chartData = useMemo(() => {
      const chartDataResult = eventChartData ? [..._chartDataResult, eventChartData] : _chartDataResult;

      return defineSingleDateBarWidth(chartDataResult, config, effectiveTimerange?.from, effectiveTimerange?.to);
    }, [_chartDataResult, config, effectiveTimerange?.from, effectiveTimerange?.to, eventChartData]);

    const getChartLayoutSettingsWithCustomUnits = useChartLayoutSettingsWithCustomUnits({ config, chartData, barmode });

    const layout = useMemo<Partial<Layout>>(() => {
      const _layouts: Partial<Layout> = {};

      if (shapes) {
        _layouts.shapes = shapes;
      }

      if (barmode) {
        _layouts.barmode = barmode;
      }

      return { ..._layouts, ...getChartLayoutSettingsWithCustomUnits() };
    }, [shapes, barmode, getChartLayoutSettingsWithCustomUnits]);

    const [gd, setGd] = useState<any>(null);
    const [opened, setOpened] = useState(false);
    const [anchorPos, setAnchorPos] = useState({ left: 0, top: 0 });
    const [text, setText] = useState('');

    const openAtDataPoint = (e: any) => {
      if (!gd) return;
      console.log({ e });
      const pt = e.points?.[0];
      if (!pt) return;

      const fl = gd._fullLayout;
      const xa = fl[pt.xaxis?._name ?? 'xaxis'];
      const ya = fl[pt.yaxis?._name ?? 'yaxis'];

      const categoryIndex = xa._categories.indexOf(pt.x);
      const xPx = xa.l2p(categoryIndex) + fl._size.l;
      const yPx = ya.l2p(pt.y) + fl._size.t;

      console.log({
        'xa._categories': xa._categories,
        categoryIndex,
        'xa.l2p(pt.x)': xa.l2p(pt.x),
        '!!!!!!!pt.x': pt.x,
        xPx,
        yPx,
        xa,
        ya,
        fl,
      });

      const { pageX, pageY } = e.event;
      setAnchorPos({
        left: pageX,
        top: pageY,
      });
      // setAnchorPos({ left: xPx, top: yPx });
      setText(`X: ${pt.x}, Y: ${pt.y}`);
      setOpened(true);
    };

    const [anchorEl, setAnchorEl] = useState<AnchorEl>(null);

    // const rect = useAnchorRect(anchorEl);

    const onClick = (e: any) => {
      console.log({ e });
      // Find the actual bar <rect> that was clicked
      let el: Element | null = e?.event?.target as Element | null;
      console.log({ el });
      if (el && el.tagName.toLowerCase() !== 'rect') {
        el = el.closest('rect'); // walk up to the bar rect
      }
      if (!el) {
        // Fallback: bind to the overall point group if rect not found
        el = (e?.event?.target as Element | null)?.closest('.point, .pointbar');
      }

      const pt = e.points?.[0];
      setText(pt ? `X: ${String(pt.x)} • Y: ${String(pt.y)}` : '—');
      setAnchorEl(el);
      setOpened(true);
    };

    const graphDivRef = useRef<HTMLElement | null>(null);
    const [anchor, setAnchor] = useState<Anchor>(null);
    const [pos, setPos] = useState<{ left: number; top: number } | null>({ left: 0, top: 0 });
    console.log({ pos, text });

    const positionToAnchor = useCallback(() => {
      console.log({ positionToAnchor: anchor });
      if (!anchor) return setPos({ left: 0, top: 0 });
      const r = anchor.getBoundingClientRect();
      setPos({ left: r.left + r.width / 2, top: r.top });
    }, [anchor]);

    /*
    useLayoutEffect(() => {
      if (!anchor) return;
      // positionToAnchor(); // align immediately on anchor change
      const fn = () => {
        console.log('scroll');

        return positionToAnchor();
      };
      window.addEventListener('scroll', fn, { passive: true });
      document.addEventListener('scroll', fn, { passive: true, capture: true });

      return () => {
        window.removeEventListener('scroll', fn);
        window.removeEventListener('resize', fn);
      };
    }, [anchor]);

    const handleClick = (e: any) => {
      const pt = e.points?.[0];
      const graphDiv = graphDivRef.current ?? (e.event?.target as HTMLElement)?.closest('.js-plotly-plot');
      if (!pt || !graphDiv) return;

      const el = getBarElement(graphDiv, pt.curveNumber, pt.pointIndex);
      console.log({ el });
      setAnchor(el);
      setText(`X: ${String(pt.x)} • Y: ${String(pt.y)}`);
      setOpened(true);
      const r = el.getBoundingClientRect();
      console.log({ left: r.left + r.width / 2, top: r.top });
      setPos({ left: r.left + r.width / 2, top: r.top });
      // requestAnimationFrame(positionToAnchor);
    };
*/
    const setFromRect = (el: Element) => {
      const r = el.getBoundingClientRect();
      setPos({ left: r.left + r.width / 2, top: r.top });
    };

    console.log({
      anchor,
    });
    useLayoutEffect(() => {
      if (!anchor) return;
      let prev = { left: -1, top: -1, w: -1, h: -1 };
      let raf = 0;

      const tick = () => {
        if (!anchor) return;
        const r = anchor.getBoundingClientRect();
        // update only if changed to avoid extra renders
        if (r.left !== prev.left || r.top !== prev.top || r.width !== prev.w || r.height !== prev.h) {
          prev = { left: r.left, top: r.top, w: r.width, h: r.height };
          setPos({ left: r.left + r.width / 2, top: r.top });
        }
        raf = requestAnimationFrame(tick);
      };

      // kick it off
      tick();

      return () => cancelAnimationFrame(raf);
    }, [anchor]);

    const handleClick = (e: any) => {
      const pt = e.points?.[0];
      const graphDiv = graphDivRef.current ?? (e.event?.target as HTMLElement)?.closest('.js-plotly-plot');
      if (!pt || !graphDiv) return;

      const el = getBarElement(graphDiv, pt.curveNumber, pt.pointIndex);
      if (!el) return;

      // position immediately so the first render is correct
      setFromRect(el);
      setAnchor(el);
      setOpened(true);
      setText(`X: ${String(pt.x)} • Y: ${String(pt.y)}`);
    };

    const onClosePopover = () => {
      setAnchor(null);
      setPos(null);
    };

    const isModalOpen = !!anchor && !!pos;

    return (
      <>
        <XYPlot
          config={config}
          axisType={visualizationConfig.axisType}
          chartData={chartData}
          effectiveTimerange={effectiveTimerange}
          setChartColor={setChartColor}
          height={height}
          width={width}
          plotLayout={layout}
          onClickMarker={handleClick}
          onInitialized={(_fig, gd: any) => {
            graphDivRef.current = gd as HTMLElement;
          }}
          // onInitialized={(_fig, graphDiv) => setGd(graphDiv)}
        />
        <Popover
          onClose={onClosePopover}
          opened={isModalOpen}
          onChange={setOpened}
          withArrow
          withinPortal
          position="top"
          offset={8}>
          <Popover.Target>
            <div
              style={{
                position: 'fixed',
                // left: anchorPos.left,
                // top: anchorPos.top,
                left: pos.left,
                top: pos.top,
                width: 1,
                height: 1,
              }}
            />
          </Popover.Target>
          <Popover.Dropdown>
            <span>{text}</span>
          </Popover.Dropdown>
        </Popover>
      </>
    );
  },
  'bar',
);

export default BarVisualization;
