import React from 'react';
import { trim } from 'lodash';

import Spinner from 'components/common/Spinner';
import { FieldList } from 'enterprise/components/sidebar';
import SearchBar from 'enterprise/components/SearchBar';
import ParameterBar from 'enterprise/components/parameters/ParameterBar';
import Query from 'enterprise/components/Query';
import SideBar from 'enterprise/components/sidebar/SideBar';

import connect from 'stores/connect';

import { FieldTypesStore } from 'enterprise/stores/FieldTypesStore';
import { SearchStore } from 'enterprise/stores/SearchStore';
import { CurrentViewStateStore } from 'enterprise/stores/CurrentViewStateStore';
import { SelectedFieldsStore } from 'enterprise/stores/SelectedFieldsStore';
import { getUndeclaredParameters, getUsedParameters, SearchMetadataStore } from 'enterprise/stores/SearchMetadataStore';
import { ViewMetadataStore } from 'enterprise/stores/ViewMetadataStore';
import { WidgetStore } from 'enterprise/stores/WidgetStore';
import LoadingIndicator from 'components/common/LoadingIndicator';
import { getParameterBindingsAsMap } from 'enterprise/stores/SearchExecutionStateStore';
import { SearchLoadingStateStore } from '../stores/SearchLoadingStateStore';
import { SearchConfigStore } from '../stores/SearchConfigStore';

const _disableSearch = (undeclaredParameters, parameterBindings) => {
  const bindingsMap = getParameterBindingsAsMap(parameterBindings);
  const missingValues = bindingsMap.filter(value => !trim(value));

  return undeclaredParameters.size > 0 || missingValues.size > 0;
};

const SearchLoadingIndicator = connect(
  ({ searchLoadingState }) => (searchLoadingState.isLoading && <LoadingIndicator text="Updating search results..." />),
  { searchLoadingState: SearchLoadingStateStore },
);

const ConnectedFieldList = connect(FieldList, { selectedFields: SelectedFieldsStore });
const ConnectedSideBar = connect(SideBar, { viewMetadata: ViewMetadataStore });
const QueryWithWidgets = connect(Query, { widgets: WidgetStore });

const SearchResult = (props) => {
  const { parameterBindings, queryId } = props;
  const { configurations, fieldTypes, searches, searchMetadata, showMessages, viewState } = props;
  const { onExecute, onHandleParameterSave, onToggleMessages } = props;

  if (!configurations.searchesClusterConfig || !fieldTypes) {
    return <Spinner />;
  }

  const results = searches && searches.result;
  const widgetMapping = searches && searches.widgetMapping;
  const searchConfig = configurations.searchesClusterConfig;

  const currentResults = results ? results.forId(queryId) : undefined;
  const queryFields = fieldTypes.queryFields.get(queryId, fieldTypes.all);
  const positions = viewState.state && viewState.state.widgetPositions;

  const usedParameters = getUsedParameters(searchMetadata);
  const undeclaredParameters = getUndeclaredParameters(searchMetadata);
  const disableSearch = _disableSearch(undeclaredParameters, parameterBindings);

  const content = currentResults ? (
    <QueryWithWidgets fields={queryFields}
                      onToggleMessages={onToggleMessages}
                      queryId={queryId}
                      results={currentResults}
                      showMessages={showMessages}
                      positions={positions}
                      widgetMapping={widgetMapping}>
      <ConnectedSideBar queryId={queryId} results={currentResults}>
        <ConnectedFieldList allFields={fieldTypes.all}
                            fields={queryFields} />
      </ConnectedSideBar>
    </QueryWithWidgets>
  ) : <Spinner />;

  return (
    <span>
      <SearchBar config={searchConfig}
                 results={currentResults}
                 disableSearch={disableSearch}
                 onExecute={onExecute} />
      <ParameterBar usedParameters={usedParameters}
                    undeclaredParameters={undeclaredParameters}
                    onParameterSave={onHandleParameterSave} />
      {content}
      <SearchLoadingIndicator />
    </span>
  );
};

export default connect(SearchResult, {
  configurations: SearchConfigStore,
  fieldTypes: FieldTypesStore,
  searches: SearchStore,
  searchMetadata: SearchMetadataStore,
  viewState: CurrentViewStateStore,
});
