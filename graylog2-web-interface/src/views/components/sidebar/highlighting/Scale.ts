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
import * as chroma from 'chroma-js';

const plotlyScaleToChroma = (plotlyScale: Array<[domain: number, color: string]>) => {
  const domains = plotlyScale.map(([domain]) => domain);
  const colors = plotlyScale.map(([, color]) => color);

  return chroma.scale(colors).domain(domains);
};

const scaleForGradient = (gradient: string): chroma.Scale => {
  switch (gradient) {
    case 'Blackbody':
    case 'Bluered':
    case 'Cividis':
    case 'Earth':
    case 'Electric':
    case 'Hot':
    case 'Jet':
    case 'Picnic':
    case 'Portland':
    case 'Rainbow': return plotlyScaleToChroma(scales[gradient]);
    default: return chroma.scale(gradient);
  }
};

export default scaleForGradient;
