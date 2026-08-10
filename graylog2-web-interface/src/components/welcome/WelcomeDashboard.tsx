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
import { forwardRef, useMemo } from 'react';
import styled from 'styled-components';

import generateId from 'logic/generateId';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import { BLANK } from 'views/components/contexts/SearchPageLayoutContext';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import SearchPage from 'views/pages/SearchPage';
import useCreateSearch from 'views/hooks/useCreateSearch';
import View from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';
import Search from 'views/logic/search/Search';
import QueryGenerator from 'views/logic/queries/QueryGenerator';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import FieldType from 'views/logic/fieldtypes/FieldType';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { widgetDragHandleClass } from 'views/components/widgets/Constants';
import { TIMESTAMP_FIELD } from 'views/Constants';
import WidgetActionsContext from 'views/components/widgets/WidgetActionsContext';
import DefaultChartColorsContext from 'views/components/widgets/DefaultChartColorsContext';

import replayLinkWidgetAction from './ReplayLinkWidgetAction';

const LAST_24_HOURS = { type: 'relative' as const, from: 86400 };
const WIDGET_ACTIONS = [replayLinkWidgetAction];

const MESSAGES_TODAY_LINK = '/search?q=&rangetype=relative&from=300';
const ALERTS_LINK =
  '/alerts?page=1&filters=priority%3D4&filters=priority%3D3&filters=priority%3D2&filters=priority%3D1&filters=timestamp%3Drelative%4086400&filters=alert%3Dtrue';
const EVENTS_LINK =
  '/alerts?page=1&filters=priority%3D4&filters=priority%3D3&filters=priority%3D2&filters=priority%3D1&filters=timestamp%3Drelative%4086400&filters=alert%3Dfalse';
const ALERTS_EVENTS_STREAMS = ['000000000000000000000003', '000000000000000000000002'];
// TODO: replace with colors from the styled-components theme once available.
const AREA_CHART_COLORS = ['#0C50A5', '#9A6BFE', '#4396FF', '#03C2FF', '#C2F0FF'];

const StyledSearchContainer = styled.div`
  footer,
  .query-tab-create,
  .query-config-btn,
  .fa-star,
  .react-resizable-handle,
  button:has(.fa-copy),
  button:has(.fa-chevron-down),
  .${widgetDragHandleClass} {
    display: none;
  }
`;

const Container = styled.div`
  margin-bottom: 6.4px;
`;

const SearchAreaContainer = forwardRef<HTMLDivElement, React.PropsWithChildren>(({ children }, ref) => (
  <StyledSearchContainer ref={ref}>{children}</StyledSearchContainer>
));

type NumberWidgetOptions = {
  title: string;
  link: string;
  queryString?: string;
  streams?: Array<string>;
};

const numberWidget = ({ title, link, queryString = '', streams = [] }: NumberWidgetOptions) => ({
  title,
  widget: AggregationWidget.builder()
    .id(generateId())
    .timerange(LAST_24_HOURS)
    .query(createElasticsearchQueryString(queryString))
    .streams(streams)
    .context(link)
    .config(
      AggregationWidgetConfig.builder()
        .series([Series.forFunction('count()')])
        .visualization('numeric')
        .visualizationConfig(NumberVisualizationConfig.create(true, 'NEUTRAL'))
        .rollup(false)
        .build(),
    )
    .build(),
});

const topSourcesWidget = () => ({
  title: 'Top 5 Sources',
  widget: AggregationWidget.builder()
    .id(generateId())
    .timerange(LAST_24_HOURS)
    .query(createElasticsearchQueryString('NOT source:example.org'))
    .config(
      AggregationWidgetConfig.builder()
        .rowPivots([pivotForField(TIMESTAMP_FIELD, new FieldType('date', [], []))])
        .columnPivots([Pivot.createValues(['source'], { limit: 5, skip_empty_values: false })])
        .series([Series.forFunction('count()')])
        .visualization('area')
        .visualizationConfig(AreaVisualizationConfig.create('spline'))
        .rollup(false)
        .build(),
    )
    .build(),
});

const buildViewState = () => {
  const entries = [
    numberWidget({ title: 'Messages Today', link: MESSAGES_TODAY_LINK }),
    numberWidget({
      title: 'Alerts Today',
      link: ALERTS_LINK,
      queryString: 'alert:true',
      streams: ALERTS_EVENTS_STREAMS,
    }),
    numberWidget({
      title: 'Events Today',
      link: EVENTS_LINK,
      queryString: 'alert:false',
      streams: ALERTS_EVENTS_STREAMS,
    }),
    topSourcesWidget(),
  ];
  const [first, second, third, sources] = entries;

  return ViewState.create()
    .toBuilder()
    .titles({ widget: Object.fromEntries(entries.map(({ widget, title }) => [widget.id, title])) })
    .widgets(entries.map(({ widget }) => widget))
    .widgetPositions({
      [first.widget.id]: new WidgetPosition(1, 1, 1.6, 4),
      [second.widget.id]: new WidgetPosition(5, 1, 1.6, 4),
      [third.widget.id]: new WidgetPosition(9, 1, 1.6, 4),
      [sources.widget.id]: new WidgetPosition(1, 2.75, 3, 12),
    })
    .build();
};

const buildWelcomeDashboard = () => {
  const query = QueryGenerator(undefined, undefined, undefined, LAST_24_HOURS);
  const search = Search.create().toBuilder().queries([query]).build();
  const view = View.create()
    .toBuilder()
    .newId()
    .type(View.Type.Dashboard)
    .state({ [query.id]: buildViewState() })
    .search(search)
    .build();

  return Promise.resolve(UpdateSearchForWidgets(view));
};

const WelcomeDashboard = () => {
  const viewPromise = useMemo(() => buildWelcomeDashboard(), []);
  const view = useCreateSearch(viewPromise);

  const searchPageLayoutContextValue = useMemo(
    () => ({
      sidebar: { isShown: false },
      viewActions: BLANK,
      searchAreaContainer: { component: SearchAreaContainer },
    }),
    [],
  );

  return (
    <Container>
      <InteractiveContext.Provider value={false}>
        <WidgetActionsContext.Provider value={WIDGET_ACTIONS}>
          <DefaultChartColorsContext.Provider value={AREA_CHART_COLORS}>
            <SearchPageLayoutProvider value={searchPageLayoutContextValue}>
              <SearchPage view={view} isNew={false} skipNoStreamsCheck />
            </SearchPageLayoutProvider>
          </DefaultChartColorsContext.Provider>
        </WidgetActionsContext.Provider>
      </InteractiveContext.Provider>
    </Container>
  );
};

export default WelcomeDashboard;
