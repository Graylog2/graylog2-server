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

import type { WidgetComponentProps } from 'views/types';
import connect from 'stores/connect';
import { Messages } from 'views/Constants';
import { ViewStore } from 'views/stores/ViewStore';
import type { SearchStoreState } from 'views/stores/SearchStore';
import { SearchActions, SearchStore } from 'views/stores/SearchStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import type MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import type { TimeRange } from 'views/logic/queries/Query';
import type { ViewStoreState } from 'views/stores/ViewStore';
import type { SearchTypeOptions } from 'views/logic/search/GlobalOverride';
import { PaginatedList } from 'components/common';
import MessageTable from 'views/components/widgets/MessageTable';
import ErrorWidget from 'views/components/widgets/ErrorWidget';
import type SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import type { BackendMessage } from 'views/components/messagelist/Types';
import type Widget from 'views/logic/widgets/Widget';
import WindowDimensionsContextProvider from 'contexts/WindowDimensionsContextProvider';
import { InputsActions } from 'stores/inputs/InputsStore';

import RenderCompletionCallback from './RenderCompletionCallback';

const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;

  .pagination {
    margin-bottom: 0;
  }
`;

type State = {
  errors: Array<{ description: string }>,
  currentPage: number,
};

export type MessageListResult = {
  messages: Array<BackendMessage>,
  total: number,
  id: string,
  type: 'messages'
};

type SearchType = { effectiveTimerange: TimeRange };
type Props = WidgetComponentProps<MessagesWidgetConfig, MessageListResult> & {
  currentView: ViewStoreState,
  pageSize?: number,
  searchTypes: { [searchTypeId: string]: SearchType },
};

class MessageList extends React.Component<Props, State> {
  static defaultProps = {
    onConfigChange: () => Promise.resolve(Immutable.OrderedMap<string, Widget>()),
    pageSize: Messages.DEFAULT_LIMIT,
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
    const searchTypePayload: SearchTypeOptions<{
      limit: number,
      offset: number,
    }> = {
      [searchTypeId]: {
        limit: pageSize,
        offset: pageSize * (pageNo - 1),
      },
    };

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

    return onConfigChange(newConfig).then(() => {});
  };

  render() {
    const {
      config,
      currentView: { activeQuery: activeQueryId },
      data: { messages, total: totalMessages },
      fields,
      pageSize,
      setLoadingState,
    } = this.props;
    const { currentPage, errors } = this.state;
    const hasError = !isEmpty(errors);
    const listKey = this._getListKey();

    return (
      <WindowDimensionsContextProvider>
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
                            setLoadingState={setLoadingState}
                            messages={messages} />
            ) : <ErrorWidget errors={errors} />}
          </PaginatedList>
        </Wrapper>
      </WindowDimensionsContextProvider>
    );
  }
}

const mapProps = (props: {
  currentView: ViewStoreState,
  searches: SearchStoreState,
}) => ({
  currentView: props.currentView,
  searchTypes: get(props, ['searches', 'result', 'results', props.currentView.activeQuery, 'searchTypes']) as { [searchTypeId: string]: SearchType },
});

export default connect(MessageList, {
  currentView: ViewStore,
  searches: SearchStore,
}, mapProps);
