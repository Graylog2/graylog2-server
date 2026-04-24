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
import chroma from 'chroma-js';

type ScaleDef = Array<[domain: number, color: string]>;

/**
 * Colorscales previously provided by plotly.js. Inlined here to remove the plotly dependency.
 * Source: https://github.com/plotly/plotly.js/blob/master/src/components/colorscale/scales.js
 */
const SCALES: Record<string, ScaleDef> = {
  Blackbody: [
    [0, 'rgb(0,0,0)'],
    [0.2, 'rgb(230,0,0)'],
    [0.4, 'rgb(230,210,0)'],
    [0.7, 'rgb(255,255,255)'],
    [1, 'rgb(160,200,255)'],
  ],
  Bluered: [
    [0, 'rgb(0,0,255)'],
    [1, 'rgb(255,0,0)'],
  ],
  Cividis: [
    [0, 'rgb(0,32,76)'],
    [0.13, 'rgb(0,67,96)'],
    [0.25, 'rgb(57,89,100)'],
    [0.38, 'rgb(92,107,104)'],
    [0.5, 'rgb(122,126,110)'],
    [0.63, 'rgb(155,147,98)'],
    [0.75, 'rgb(192,170,67)'],
    [0.88, 'rgb(231,197,22)'],
    [1, 'rgb(253,231,37)'],
  ],
  Earth: [
    [0, 'rgb(0,0,130)'],
    [0.1, 'rgb(0,180,180)'],
    [0.2, 'rgb(40,210,40)'],
    [0.4, 'rgb(230,220,50)'],
    [0.6, 'rgb(120,100,50)'],
    [1, 'rgb(255,255,255)'],
  ],
  Electric: [
    [0, 'rgb(0,0,0)'],
    [0.15, 'rgb(30,0,100)'],
    [0.4, 'rgb(120,0,100)'],
    [0.6, 'rgb(160,90,0)'],
    [0.8, 'rgb(230,200,0)'],
    [1, 'rgb(255,250,220)'],
  ],
  Hot: [
    [0, 'rgb(0,0,0)'],
    [0.3, 'rgb(230,0,0)'],
    [0.6, 'rgb(255,210,0)'],
    [1, 'rgb(255,255,255)'],
  ],
  Jet: [
    [0, 'rgb(0,0,131)'],
    [0.125, 'rgb(0,60,170)'],
    [0.375, 'rgb(5,255,255)'],
    [0.625, 'rgb(255,255,0)'],
    [0.875, 'rgb(250,0,0)'],
    [1, 'rgb(128,0,0)'],
  ],
  Picnic: [
    [0, 'rgb(0,0,255)'],
    [0.1, 'rgb(51,153,255)'],
    [0.2, 'rgb(102,204,255)'],
    [0.3, 'rgb(153,204,255)'],
    [0.4, 'rgb(204,204,255)'],
    [0.5, 'rgb(255,255,255)'],
    [0.6, 'rgb(255,204,255)'],
    [0.7, 'rgb(255,153,255)'],
    [0.8, 'rgb(255,102,204)'],
    [0.9, 'rgb(255,102,102)'],
    [1, 'rgb(255,0,0)'],
  ],
  Portland: [
    [0, 'rgb(12,51,131)'],
    [0.25, 'rgb(10,136,186)'],
    [0.5, 'rgb(242,211,56)'],
    [0.75, 'rgb(242,143,56)'],
    [1, 'rgb(217,30,30)'],
  ],
  Rainbow: [
    [0, 'rgb(150,0,90)'],
    [0.125, 'rgb(0,0,200)'],
    [0.25, 'rgb(0,25,255)'],
    [0.375, 'rgb(0,152,255)'],
    [0.5, 'rgb(44,255,150)'],
    [0.625, 'rgb(151,255,0)'],
    [0.75, 'rgb(255,234,0)'],
    [0.875, 'rgb(255,111,0)'],
    [1, 'rgb(255,0,0)'],
  ],
};

const plotlyScaleToChroma = (plotlyScale: ScaleDef) => {
  const domains = plotlyScale.map(([domain]) => domain);
  const colors = plotlyScale.map(([, color]) => color);

  return chroma.scale(colors).domain(domains);
};

const scaleForGradient = (gradient: string): chroma.Scale => {
  const scaleDef = SCALES[gradient];

  if (scaleDef) {
    return plotlyScaleToChroma(scaleDef);
  }

  return chroma.scale(gradient);
};

export default scaleForGradient;
