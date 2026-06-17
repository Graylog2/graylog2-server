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
import type { PlotMouseEvent, PlotlyHTMLElement } from 'plotly.js';

import type { ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';
import type { PlotlyHTMLElementWithInternals } from 'views/components/visualizations/OnClickPopover/anchors';
import { makeElementAnchor } from 'views/components/visualizations/OnClickPopover/anchors';
import dropdownPopover from 'views/components/visualizations/OnClickPopover/dropdownPopover';
import CartesianOnClickPopoverDropdown from 'views/components/visualizations/OnClickPopover/CartesianOnClickPopoverDropdown';

const getBarElement = (
  graphDiv: HTMLElement,
  pt: ClickPoint,
  curveNumberRenderIndexMapper: Record<number, number>,
): Element | null => {
  const { curveNumber, pointIndex } = pt;
  const traceIndex = curveNumberRenderIndexMapper[curveNumber];
  const trace = graphDiv.querySelectorAll('.cartesianlayer .trace')[traceIndex] as HTMLElement | undefined;

  if (!trace) return null;
  const point = trace.querySelectorAll('.point')[pointIndex!] as HTMLElement | undefined;

  return point?.querySelector('rect') ?? point?.querySelector('path') ?? null;
};

const listSubplotOrder = (gd: HTMLElement): string[] => {
  const subplots = gd.querySelectorAll<SVGGElement>('.cartesianlayer g.subplot');

  return Array.from(subplots).map((node) => {
    // Each subplot node has classes like "subplot xy" or "subplot x2y3"
    // We want the specific id class (not the generic "subplot")
    const idClass = Array.from(node.classList).find((cls) => cls !== 'subplot');

    return idClass ?? '';
  });
};

const createBarElementGetter = (gd: PlotlyHTMLElementWithInternals) => {
  const fullData = gd._fullData;
  const curveNumberGroupedBySubPlot: Record<string, number[]> = {};

  fullData.forEach(({ xaxis, yaxis }, curveNumber) => {
    const subplot = `${xaxis ?? 'x'}${yaxis ?? 'y'}`;
    (curveNumberGroupedBySubPlot[subplot] ||= []).push(curveNumber);
  });
  const subPlotDOMOrder = listSubplotOrder(gd);

  const curveNumberRenderIndexArray = subPlotDOMOrder.flatMap((subPlotId) => curveNumberGroupedBySubPlot[subPlotId]);
  const curveNumberRenderIndexMapper = Object.fromEntries(curveNumberRenderIndexArray.map((v, i) => [v, i]));

  return (graphDiv: HTMLElement, pt: ClickPoint) => getBarElement(graphDiv, pt, curveNumberRenderIndexMapper);
};

const buildAnchor = (e: PlotMouseEvent, gd: PlotlyHTMLElement) => makeElementAnchor(e, gd, createBarElementGetter(gd));

const barOnClickPopover = {
  buildAnchor,
  renderPopover: dropdownPopover(CartesianOnClickPopoverDropdown),
};

export default barOnClickPopover;
