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
import { useContext } from 'react';
import styled, { css } from 'styled-components';

import Spinner from 'components/common/Spinner';
import Query from 'views/components/Query';
import connect from 'stores/connect';
import { SearchStore } from 'views/stores/SearchStore';
import { CurrentViewStateStore } from 'views/stores/CurrentViewStateStore';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { WidgetStore } from 'views/stores/WidgetStore';
import { SearchLoadingStateStore } from 'views/stores/SearchLoadingStateStore';
import type { QueryId } from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';
import TSearchResult from 'views/logic/SearchResult';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import LoadingIndicator from 'components/common/LoadingIndicator';
import { Row, Col } from 'components/graylog';

type IndicatorProps = {
  searchLoadingState: {
    isLoading: boolean;
  };
};

const StyledRow = styled(Row)(({ hasFocusedWidget }) => css`
  height: 100%;
  ${hasFocusedWidget && 'overflow: auto'}
`);

const SearchLoadingIndicator = connect(
  ({ searchLoadingState }: IndicatorProps) => (searchLoadingState.isLoading && <LoadingIndicator text="Updating search results..." />),
  { searchLoadingState: SearchLoadingStateStore },
);

const QueryWithWidgets = connect(Query, { widgets: WidgetStore });

type Props = {
  queryId: QueryId,
  searches: TSearchResult,
  viewState: {
    state: ViewState,
    activeQuery: QueryId,
    focusedWidget: string | undefined | null,
  },
};

const SearchResult = React.memo(({ queryId, searches, viewState }: Props) => {
  const fieldTypes = useContext(FieldTypesContext);

  if (!fieldTypes) {
    return <Spinner />;
  }

  const results = searches && searches.result;
  const widgetMapping = searches && searches.widgetMapping;
  const hasFocusedWidget = !!viewState.focusedWidget;

  const currentResults = results ? results.forId(queryId) : undefined;
  const allFields = fieldTypes.all;
  const queryFields = fieldTypes.queryFields.get(queryId, fieldTypes.all);
  const positions = viewState.state && viewState.state.widgetPositions;
  const content = currentResults ? (
    <QueryWithWidgets allFields={allFields}
                      fields={queryFields}
                      queryId={queryId}
                      results={currentResults}
                      positions={positions}
                      widgetMapping={widgetMapping} />
  ) : <Spinner />;

  return (
    <StyledRow hasFocusedWidget={hasFocusedWidget}>
      <Col style={{ height: '100%' }}>
        {content}
        <SearchLoadingIndicator />
      </Col>
    </StyledRow>
  );
});

export default connect(SearchResult, {
  searches: SearchStore,
  viewMetadata: ViewMetadataStore,
  viewState: CurrentViewStateStore,
}, (props) => ({
  ...props,
  searches: { result: props.searches.result, widgetMapping: props.searches.widgetMapping },
  queryId: props.viewMetadata.activeQuery,
}));
