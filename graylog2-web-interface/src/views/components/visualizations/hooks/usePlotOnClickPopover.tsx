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
import { useRef, useState, useCallback, useMemo } from 'react';
import { useFloating } from '@floating-ui/react';

import type { PlotMouseEvent, EChartsInstance } from 'views/components/visualizations/types';
import type { ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';
import type { OnClickMarkerEvent } from 'views/components/visualizations/GenericPlot';
import OnClickPopoverWrapper from 'views/components/visualizations/OnClickPopover/OnClickPopoverWrapper';
import CartesianOnClickPopoverDropdown from 'views/components/visualizations/OnClickPopover/CartesianOnClickPopoverDropdown';
import HeatmapOnClickPopover from 'views/components/visualizations/OnClickPopover/HeatmapOnClickPopover';
import PieOnClickPopoverDropdown from 'views/components/visualizations/OnClickPopover/PieOnClickPopoverDropdown';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DropdownSwitcher from 'views/components/visualizations/OnClickPopover/DropdownSwitcher';

type ChartType = 'bar' | 'scatter' | 'pie' | 'heatmap';

type Anchor = {
  el: Element;
  rel: { x: number; y: number };
  pt: ClickPoint;
  pointsInRadius?: Array<ClickPoint>;
};

const popoverComponent = (chartType: ChartType) => {
  switch (chartType) {
    case 'heatmap':
      return HeatmapOnClickPopover;
    case 'pie':
      return PieOnClickPopoverDropdown;
    default:
      return CartesianOnClickPopoverDropdown;
  }
};

const usePlotOnClickPopover = (chartType: ChartType, config: AggregationWidgetConfig) => {
  const chartInstanceRef = useRef<EChartsInstance | null>(null);
  const virtualElRef = useRef<HTMLDivElement | null>(null);
  const [anchor, setAnchor] = useState<Anchor | null>(null);
  const { refs, floatingStyles } = useFloating({
    placement: 'top-start',
    elements: {
      reference: anchor?.el,
    },
    transform: false,
  });

  const initializeGraphDivRef = useCallback((_: unknown, instance: EChartsInstance) => {
    chartInstanceRef.current = instance;

    // Create a virtual anchor element positioned by click coordinates
    const dom = instance.getDom();

    if (dom && !virtualElRef.current) {
      const el = document.createElement('div');
      el.style.position = 'absolute';
      el.style.width = '1px';
      el.style.height = '1px';
      el.style.pointerEvents = 'none';
      dom.style.position = 'relative';
      dom.appendChild(el);
      virtualElRef.current = el;
    }
  }, []);

  const onPopoverClose = useCallback(() => setAnchor(null), []);

  const onChartClick = useCallback(
    (_: OnClickMarkerEvent, e: PlotMouseEvent) => {
      if (!e?.points?.[0]) return;

      const pt = e.points[0] as ClickPoint;
      const mouseEvent = e.event as MouseEvent;

      // Position the virtual anchor at the click location
      if (virtualElRef.current && mouseEvent) {
        const dom = chartInstanceRef.current?.getDom();

        if (dom) {
          const rect = dom.getBoundingClientRect();
          const x = (mouseEvent.clientX ?? mouseEvent.pageX ?? 0) - rect.left;
          const y = (mouseEvent.clientY ?? mouseEvent.pageY ?? 0) - rect.top;
          virtualElRef.current.style.left = `${x}px`;
          virtualElRef.current.style.top = `${y}px`;
        }
      }

      setAnchor({
        el: virtualElRef.current,
        rel: { x: 0, y: 0 },
        pt,
        pointsInRadius: [pt],
      });
    },
    [],
  );

  const onPopoverChange = useCallback(
    (isOpen: boolean) => {
      if (!isOpen) onPopoverClose();
    },
    [onPopoverClose],
  );

  const isPopoverOpen = !!anchor;

  const PopoverComponent = useMemo(() => popoverComponent(chartType), [chartType]);

  const popover = (
    <OnClickPopoverWrapper
      isPopoverOpen={isPopoverOpen}
      onPopoverChange={onPopoverChange}
      ref={refs.setFloating}
      style={floatingStyles}>
      <DropdownSwitcher
        component={PopoverComponent}
        clickPoint={anchor?.pt}
        config={config}
        clickPointsInRadius={anchor?.pointsInRadius}
        onPopoverClose={onPopoverClose}
      />
    </OnClickPopoverWrapper>
  );

  return { initializeGraphDivRef, onChartClick, popover };
};

export default usePlotOnClickPopover;
