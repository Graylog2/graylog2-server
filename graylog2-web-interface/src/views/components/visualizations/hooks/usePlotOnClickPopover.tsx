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
import { useRef, useState } from 'react';
import type { PlotMouseEvent, PlotlyHTMLElement } from 'plotly.js';
import { useFloating } from '@floating-ui/react';

import type { Rel } from 'views/components/visualizations/OnClickPopover/Types';
import type { OnClickMarkerEvent } from 'views/components/visualizations/GenericPlot';
import OnClickPopoverWrapper from 'views/components/visualizations/OnClickPopover/OnClickPopoverWrapper';
import type { Anchor, BuildAnchor, RenderPopover } from 'views/components/visualizations/OnClickPopover/anchors';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

type OnClickPopoverInput = {
  buildAnchor: BuildAnchor;
  renderPopover: RenderPopover;
  config: AggregationWidgetConfig;
};

const alignByRelativeCoords = (rel: Rel = { x: 0, y: 0 }) => ({
  name: 'alignByRelativeCoords',
  options: rel,
  fn: ({ x, y, rects }) => ({
    x: x + rects.reference.width * rel.x,
    y: y + rects.reference.height * rel.y,
  }),
});

/** ---------- hook ---------- */
const usePlotOnClickPopover = ({ buildAnchor, renderPopover, config }: OnClickPopoverInput) => {
  const gdRef = useRef<PlotlyHTMLElement | null>(null);
  const [anchor, setAnchor] = useState<Anchor | null>(null);
  const { refs, floatingStyles } = useFloating({
    placement: 'top-start',
    elements: {
      reference: anchor?.el,
    },
    transform: false,
    middleware: [alignByRelativeCoords(anchor?.rel)],
  });

  const initializeGraphDivRef = (_: unknown, gd: PlotlyHTMLElement) => {
    gdRef.current = gd;
  };

  const onPopoverClose = () => setAnchor(null);

  const onChartClick = (_: OnClickMarkerEvent, e: PlotMouseEvent) => {
    const targetEl = e.event?.target;
    const targetElForClosest = targetEl instanceof Element ? targetEl : null;
    const gd = gdRef.current ?? targetElForClosest?.closest('.js-plotly-plot');
    if (!gd) return;
    const a = buildAnchor(e, gd);
    if (!a) return;
    setAnchor((prev) => {
      // Avoid recreating the anchor for the same element — a new object reference
      // would cause DropdownSwitcher's `useEffect` to reset the step back to
      // 'values' and dismiss the action menu the user just opened.
      if (prev && prev.el === a.el) return prev;

      return a;
    });
  };

  const onPopoverChange = (isOpen: boolean) => {
    if (!isOpen) onPopoverClose();
  };

  const isPopoverOpen = !!anchor;

  const popover = (
    <OnClickPopoverWrapper
      isPopoverOpen={isPopoverOpen}
      onPopoverChange={onPopoverChange}
      // eslint-disable-next-line react-hooks/refs
      ref={refs.setFloating}
      style={floatingStyles}>
      {anchor ? renderPopover({ anchor, config, onPopoverClose }) : null}
    </OnClickPopoverWrapper>
  );

  return { initializeGraphDivRef, onChartClick, popover };
};

export default usePlotOnClickPopover;
