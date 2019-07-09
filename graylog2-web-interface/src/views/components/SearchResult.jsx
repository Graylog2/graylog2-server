// @flow strict
import * as React from 'react';

import Spinner from 'components/common/Spinner';
import { FieldList } from 'views/components/sidebar';
import Query from 'views/components/Query';
import SideBar from 'views/components/sidebar/SideBar';

import connect from 'stores/connect';

import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import { SearchStore } from 'views/stores/SearchStore';
import { CurrentViewStateStore } from 'views/stores/CurrentViewStateStore';
import { SelectedFieldsStore } from 'views/stores/SelectedFieldsStore';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { WidgetStore } from 'views/stores/WidgetStore';
import LoadingIndicator from 'components/common/LoadingIndicator';
import { SearchLoadingStateStore } from 'views/stores/SearchLoadingStateStore';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { QueryId } from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';
import TSearchResult from 'views/logic/SearchResult';

const SearchLoadingIndicator = connect(
  ({ searchLoadingState }) => (searchLoadingState.isLoading && <LoadingIndicator text="Updating search results..." />),
  { searchLoadingState: SearchLoadingStateStore },
);

const ConnectedFieldList = connect(FieldList, { selectedFields: SelectedFieldsStore });
const ConnectedSideBar = connect(SideBar, { viewMetadata: ViewMetadataStore });
const QueryWithWidgets = connect(Query, { widgets: WidgetStore });

type Props = {
  fieldTypes: FieldTypeMappingsList,
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
                      widgetMapping={widgetMapping}>
      <ConnectedSideBar queryId={queryId} results={currentResults}>
        <ConnectedFieldList allFields={fieldTypes.all}
                            fields={queryFields} />
      </ConnectedSideBar>
    </QueryWithWidgets>
  ) : <Spinner />;

  return (
    <React.Fragment>
      {content}
      <SearchLoadingIndicator />
    </React.Fragment>
  );
});

export default connect(SearchResult, {
  fieldTypes: FieldTypesStore,
  searches: SearchStore,
  viewState: CurrentViewStateStore,
}, props => Object.assign({}, props, { searches: { result: props.searches.result, widgetMapping: props.searches.widgetMapping } }));
