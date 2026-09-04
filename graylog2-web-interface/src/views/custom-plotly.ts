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
import Plotly from 'plotly.js/lib/core';
import Bar from 'plotly.js/lib/bar';
import Pie from 'plotly.js/lib/pie';
import Heatmap from 'plotly.js/lib/heatmap';
import Sankey from 'plotly.js/lib/sankey';
import Scatter from 'plotly.js/lib/scatter';
import Scatterpolar from 'plotly.js/lib/scatterpolar';

// Disable plotly sankey's hard-coded 500ms transition that fades links in on
// initial render and re-fades them on node selection. The constants module is
// required by sankey/render.js; mutating the cached CommonJS exports propagates
// to every `transition().duration(c.duration)` call.
// eslint-disable-next-line @typescript-eslint/no-require-imports
const sankeyConstants: { duration: number } = require('plotly.js/src/traces/sankey/constants');
sankeyConstants.duration = 0;

// @ts-ignore
Plotly.register([Bar, Pie, Scatter, Heatmap, Sankey, Scatterpolar]);

export default Plotly;
