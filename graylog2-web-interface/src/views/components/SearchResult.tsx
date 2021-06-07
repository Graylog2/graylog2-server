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
import connect, { useStore } from 'stores/connect';
import { SearchStore } from 'views/stores/SearchStore';
import { CurrentViewStateStore } from 'views/stores/CurrentViewStateStore';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { WidgetStore } from 'views/stores/WidgetStore';
import { SearchLoadingStateStore } from 'views/stores/SearchLoadingStateStore';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import LoadingIndicator from 'components/common/LoadingIndicator';
import { Row, Col } from 'components/graylog';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

type IndicatorProps = {
  searchLoadingState: {
    isLoading: boolean;
  };
};

const StyledRow = styled(Row)(({ $hasFocusedWidget }: { $hasFocusedWidget: boolean }) => css`
  height: ${$hasFocusedWidget ? '100%' : 'auto'};
  overflow: ${$hasFocusedWidget ? 'auto' : 'visible'};
`);

const StyledCol = styled(Col)`
  height: 100%;
`;

const SearchLoadingIndicator = connect(
  ({ searchLoadingState }: IndicatorProps) => (searchLoadingState.isLoading && <LoadingIndicator text="Updating search results..." />),
  { searchLoadingState: SearchLoadingStateStore },
);

const QueryWithWidgets = connect(Query, { widgets: WidgetStore });

const SearchResult = React.memo(() => {
  const fieldTypes = useContext(FieldTypesContext);
  const { focusedWidget } = useContext(WidgetFocusContext);
  const searches = useStore(SearchStore, ({ result, widgetMapping }) => ({ result, widgetMapping }));
  const queryId = useStore(ViewMetadataStore, (viewMetadataStore) => viewMetadataStore.activeQuery);
  const viewState = useStore(CurrentViewStateStore);

  if (!fieldTypes) {
    return <Spinner />;
  }

  const results = searches && searches.result;
  const widgetMapping = searches && searches.widgetMapping;

  const hasFocusedWidget = !!focusedWidget?.id;

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
    <StyledRow $hasFocusedWidget={hasFocusedWidget}>
      <StyledCol>
        {content}
        <SearchLoadingIndicator />
      </StyledCol>
    </StyledRow>
  );
});

export default SearchResult;
