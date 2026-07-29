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
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import FieldType from 'views/logic/fieldtypes/FieldType';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { widgetDragHandleClass, widgetActionsMenuClass } from 'views/components/widgets/Constants';
import { TIMESTAMP_FIELD } from 'views/Constants';

const LAST_24_HOURS = { type: 'relative' as const, from: 86400 };

const StyledSearchContainer = styled.div`
  footer,
  .query-tab-create,
  .query-config-btn,
  .fa-star,
  .react-resizable-handle,
  button:has(.fa-copy),
  button:has(.fa-chevron-down),
  .${widgetDragHandleClass}, .${widgetActionsMenuClass} {
    display: none;
  }
`;

const Container = styled.div`
  margin-bottom: 6.4px;
`;

const SearchAreaContainer = forwardRef<HTMLDivElement, React.PropsWithChildren>(({ children }, ref) => (
  <StyledSearchContainer ref={ref}>{children}</StyledSearchContainer>
));

const numberWidget = (title: string) => ({
  title,
  widget: AggregationWidget.builder()
    .id(generateId())
    .timerange(LAST_24_HOURS)
    .config(
      AggregationWidgetConfig.builder()
        .series([Series.forFunction('count()')])
        .visualization('numeric')
        .visualizationConfig(NumberVisualizationConfig.create(true, 'HIGHER'))
        .rollup(true)
        .build(),
    )
    .build(),
});

const topSourcesWidget = () => {
  const series = Series.forFunction('count()');

  return {
    title: 'Top 5 Sources',
    widget: AggregationWidget.builder()
      .id(generateId())
      .timerange(LAST_24_HOURS)
      .config(
        AggregationWidgetConfig.builder()
          .rowPivots([pivotForField(TIMESTAMP_FIELD, new FieldType('date', [], []))])
          .columnPivots([Pivot.createValues(['source'], { limit: 5, skip_empty_values: false })])
          .series([series])
          .sort([SortConfig.fromSeries(series)])
          .visualization('bar')
          .visualizationConfig(BarVisualizationConfig.create('stack'))
          .rollup(false)
          .build(),
      )
      .build(),
  };
};

const buildViewState = () => {
  const entries = [
    numberWidget('Messages Today'),
    numberWidget('Messages Today'),
    numberWidget('Messages Today'),
    topSourcesWidget(),
  ];
  const [first, second, third, sources] = entries;

  return ViewState.create()
    .toBuilder()
    .titles({ widget: Object.fromEntries(entries.map(({ widget, title }) => [widget.id, title])) })
    .widgets(entries.map(({ widget }) => widget))
    .widgetPositions({
      [first.widget.id]: new WidgetPosition(1, 1, 2, 4),
      [second.widget.id]: new WidgetPosition(5, 1, 2, 4),
      [third.widget.id]: new WidgetPosition(9, 1, 2, 4),
      [sources.widget.id]: new WidgetPosition(1, 3, 3, 12),
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
        <SearchPageLayoutProvider value={searchPageLayoutContextValue}>
          <SearchPage view={view} isNew={false} skipNoStreamsCheck />
        </SearchPageLayoutProvider>
      </InteractiveContext.Provider>
    </Container>
  );
};

export default WelcomeDashboard;
