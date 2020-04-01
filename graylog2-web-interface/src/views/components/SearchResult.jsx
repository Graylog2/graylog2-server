// @flow strict
import * as React from 'react';

import Spinner from 'components/common/Spinner';
import Query from 'views/components/Query';

import connect from 'stores/connect';

import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import { SearchStore } from 'views/stores/SearchStore';
import { CurrentViewStateStore } from 'views/stores/CurrentViewStateStore';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { WidgetStore } from 'views/stores/WidgetStore';
import { SearchLoadingStateStore } from 'views/stores/SearchLoadingStateStore';
import type { QueryId } from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';
import TSearchResult from 'views/logic/SearchResult';
import LoadingIndicator from 'components/common/LoadingIndicator';
import { Row, Col } from 'components/graylog';
import type { FieldTypesStoreState } from '../stores/FieldTypesStore';

const SearchLoadingIndicator = connect(
  ({ searchLoadingState }) => (searchLoadingState.isLoading && <LoadingIndicator text="Updating search results..." />),
  { searchLoadingState: SearchLoadingStateStore },
);

const QueryWithWidgets = connect(Query, { widgets: WidgetStore });

type Props = {
  fieldTypes: FieldTypesStoreState,
  queryId: QueryId,
  searches: TSearchResult,
  viewState: {
    state: ViewState,
    activeQuery: QueryId,
  },
};

const SearchResult = React.memo(({ fieldTypes, queryId, searches, viewState }: Props) => {
  if (!fieldTypes) {
    return <Spinner />;
  }

  const results = searches && searches.result;
  const widgetMapping = searches && searches.widgetMapping;

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
    <Row>
      <Col>
        {content}
        <SearchLoadingIndicator />
      </Col>
    </Row>
  );
});

export default connect(SearchResult, {
  fieldTypes: FieldTypesStore,
  searches: SearchStore,
  viewMetadata: ViewMetadataStore,
  viewState: CurrentViewStateStore,
}, (props) => ({

  ...props,
  searches: { result: props.searches.result, widgetMapping: props.searches.widgetMapping },
  queryId: props.viewMetadata.activeQuery,
}));
