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

import sankeyOnClickPopover from 'views/components/visualizations/sankey/sankeyOnClickPopover';

const { buildAnchor } = sankeyOnClickPopover;

const fakeRect = (left: number, top: number, width: number, height: number) =>
  ({
    left,
    top,
    width,
    height,
    right: left + width,
    bottom: top + height,
    x: left,
    y: top,
    toJSON: () => {},
  }) as DOMRect;

const withRect = <T extends Element>(el: T, rect: DOMRect): T =>
  Object.assign(el, { getBoundingClientRect: () => rect });

// Sankey emits clicks through Fx.click(gd, { target: true }), so the PlotMouseEvent's own
// `event` carries no coordinates — the real DOM event is stashed on the point as `originalEvent`.
const clickEvent = (target: Element, clientX: number, clientY: number, pt: object): PlotMouseEvent =>
  ({
    points: [{ ...pt, originalEvent: { target, clientX, clientY } }],
    event: { target: true },
  }) as unknown as PlotMouseEvent;

describe('sankeyOnClickPopover.buildAnchor', () => {
  it('anchors the popover at the click position within a clicked link', () => {
    const gd = document.createElement('div');
    const link = withRect(document.createElement('div'), fakeRect(100, 100, 400, 200));
    link.classList.add('sankey-link');
    gd.appendChild(link);

    // Click near the top-left of the link's (large) bounding box, far from its center.
    const event = clickEvent(link, 150, 120, { source: {}, target: {}, index: 0 });
    const anchor = buildAnchor(event, gd as unknown as PlotlyHTMLElement);

    expect(anchor?.el).toBe(link);
    expect(anchor?.rel.x).toBeCloseTo((150 - 100) / 400); // 0.125
    expect(anchor?.rel.y).toBeCloseTo((120 - 100) / 200); // 0.1
  });

  it('anchors at the click position within a clicked node (resolving from a child element)', () => {
    const gd = document.createElement('div');
    const node = withRect(document.createElement('div'), fakeRect(50, 50, 20, 80));
    node.classList.add('sankey-node');
    const innerRect = document.createElement('div'); // the actual painted rect inside the node group
    node.appendChild(innerRect);
    gd.appendChild(node);

    const event = clickEvent(innerRect, 55, 90, { customdata: { field: 'action', value: 'GET' }, index: 2 });
    const anchor = buildAnchor(event, gd as unknown as PlotlyHTMLElement);

    expect(anchor?.el).toBe(node);
    expect(anchor?.rel.x).toBeCloseTo((55 - 50) / 20); // 0.25
    expect(anchor?.rel.y).toBeCloseTo((90 - 50) / 80); // 0.5
  });
});
