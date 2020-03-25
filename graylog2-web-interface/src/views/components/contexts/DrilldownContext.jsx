// @flow strict
import * as React from 'react';
import type { QueryString, TimeRange } from 'views/logic/queries/Query';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';

export type Drilldown = {
  query: QueryString,
  streams: Array<string>,
  timerange: TimeRange,
};

const defaultValue: Drilldown = {
  query: createElasticsearchQueryString(''),
  streams: [],
  timerange: { type: 'relative', range: 300 },
};

const DrilldownContext = React.createContext<Drilldown>(defaultValue);

export default DrilldownContext;
