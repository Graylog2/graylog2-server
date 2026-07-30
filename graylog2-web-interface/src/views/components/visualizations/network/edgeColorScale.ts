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
import { scales } from 'plotly.js/src/components/colorscale';
import chroma from 'chroma-js';

/**
 * Build a chroma scale from Plotly's own colorscale definition, so edge colors match the node
 * markers — which are colored by Plotly using the same named scale. (The shared `scaleForGradient`
 * helper resolves several names via ColorBrewer, whose domain order is the reverse of Plotly's for
 * scales like YlOrRd/Greys/Blues, which would map edges inversely to the nodes.)
 */
const edgeColorScale = (colorScale: string): chroma.Scale => {
  const stops = scales[colorScale] as Array<[domain: number, color: string]>;
  const domains = stops.map(([domain]) => domain);
  const colors = stops.map(([, color]) => color);

  return chroma.scale(colors).domain(domains);
};

export default edgeColorScale;
