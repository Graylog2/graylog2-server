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
import * as Immutable from 'immutable';
import styled from 'styled-components';
import { isEmpty, get } from 'lodash';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import { Messages } from 'views/Constants';
import { SelectedFieldsStore, SelectedFieldsStoreState } from 'views/stores/SelectedFieldsStore';
import { ViewStore } from 'views/stores/ViewStore';
import { SearchActions, SearchStore, SearchStoreState } from 'views/stores/SearchStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import type { TimeRange } from 'views/logic/queries/Query';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { PaginatedList } from 'components/common';
import MessageTable from 'views/components/widgets/MessageTable';
import ErrorWidget from 'views/components/widgets/ErrorWidget';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import { BackendMessage } from 'views/components/messagelist/Types';

import RenderCompletionCallback from './RenderCompletionCallback';

const { InputsActions } = CombinedProvider.get('Inputs');

const Wrapper = styled.div`
  display: grid;
  display: -ms-grid;
  -ms-grid-row: 2;
  -ms-grid-column: 1;
  grid-template-rows: 1fr auto;
  -ms-grid-rows: 1fr auto;
  -ms-grid-columns: 1fr;
  height: 100%;

  .pagination-wrapper {
    grid-row: 2;
    -ms-grid-row: 2;
    grid-column: 1;
    -ms-grid-column: 1;
  }

  .pagination {
    margin-bottom: 0;
  }
`;

type State = {
  errors: Array<{ description: string }>,
  currentPage: number,
};

type SearchType = { effectiveTimerange: TimeRange };
type Props = {
  config: MessagesWidgetConfig,
  currentView: ViewStoreState,
  data: { messages: Array<BackendMessage>, total: number, id: string },
  fields: FieldTypeMappingsList,
  onConfigChange?: (MessagesWidgetConfig) => Promise<void>,
  pageSize?: number,
  searchTypes: { [searchTypeId: string]: SearchType },
  selectedFields: Immutable.Set<string> | undefined,
  setLoadingState: (loading: boolean) => void,
};

class MessageList extends React.Component<Props, State> {
  static defaultProps = {
    onConfigChange: () => Promise.resolve(),
    pageSize: Messages.DEFAULT_LIMIT,
    selectedFields: Immutable.Set<string>(),
  };

  static contextType = RenderCompletionCallback;

  constructor(props: Props, context?: any) {
    super(props, context);

    this.state = {
      errors: [],
      currentPage: 1,
    };
  }

  componentDidMount() {
    const onRenderComplete = this.context;

    InputsActions.list().then(() => (onRenderComplete && onRenderComplete()));
    SearchActions.execute.completed.listen(this._resetPagination);
  }

  _resetPagination = () => {
    const { currentPage } = this.state;

    if (currentPage !== 1) {
      this.setState({ currentPage: 1, errors: [] });
    }
  };

  _handlePageChange = (pageNo: number) => {
    // execute search with new offset
    const { pageSize, searchTypes, data: { id: searchTypeId }, setLoadingState } = this.props;
    const { effectiveTimerange } = searchTypes[searchTypeId];
    const searchTypePayload = { [searchTypeId]: { limit: pageSize, offset: pageSize * (pageNo - 1) } };

    RefreshActions.disable();
    setLoadingState(true);

    SearchActions.reexecuteSearchTypes(searchTypePayload, effectiveTimerange).then((response) => {
      setLoadingState(false);

      this.setState({
        errors: response.result.errors,
        currentPage: pageNo,
      });
    });
  };

  _getListKey = () => {
    // When the component receives new messages, we want to reset the scroll position, by defining a new key or the MessageTable.
    const { data: { messages } } = this.props;
    const { currentPage } = this.state;
    const defaultKey = `message-list-${currentPage}`;

    if (!isEmpty(messages)) {
      const firstMessageId = messages[0].message._id;

      return `${defaultKey}-${firstMessageId}`;
    }

    return defaultKey;
  };

  _onSortChange = (newSort: SortConfig[]) => {
    const { onConfigChange, config } = this.props;
    const newConfig = config.toBuilder().sort(newSort).build();

    return onConfigChange(newConfig);
  }

  render() {
    const {
      config,
      currentView: { activeQuery: activeQueryId },
      data: { messages, total: totalMessages },
      fields,
      pageSize,
      selectedFields,
      setLoadingState,
    } = this.props;
    const { currentPage, errors } = this.state;
    const hasError = !isEmpty(errors);
    const listKey = this._getListKey();

    return (
      <Wrapper>
        <PaginatedList onChange={this._handlePageChange}
                       activePage={Number(currentPage)}
                       showPageSizeSelect={false}
                       totalItems={totalMessages}
                       pageSize={pageSize}>
          {!hasError ? (
            <MessageTable activeQueryId={activeQueryId}
                          config={config}
                          fields={fields}
                          key={listKey}
                          onSortChange={this._onSortChange}
                          selectedFields={selectedFields}
                          setLoadingState={setLoadingState}
                          messages={messages} />
          ) : <ErrorWidget errors={errors} />}
        </PaginatedList>
      </Wrapper>
    );
  }
}

const mapProps = (props: {
  selectedFields: SelectedFieldsStoreState,
  currentView: ViewStoreState,
  searches: SearchStoreState,
}) => ({
  selectedFields: props.selectedFields,
  currentView: props.currentView,
  searchTypes: get(props, ['searches', 'result', 'results', props.currentView.activeQuery, 'searchTypes']) as { [searchTypeId: string]: SearchType },
});

export default connect(MessageList, {
  selectedFields: SelectedFieldsStore,
  currentView: ViewStore,
  searches: SearchStore,
}, mapProps);
