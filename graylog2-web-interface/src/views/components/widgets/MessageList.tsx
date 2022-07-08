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
import { useContext, useState, useEffect, useCallback, useRef } from 'react';

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

const useResetScrollPositionOnPageChange = (scrollContainerRef: React.MutableRefObject<HTMLElement>, currentPage: number) => {
  useEffect(() => {
    if (scrollContainerRef.current) {
      // eslint-disable-next-line no-param-reassign
      scrollContainerRef.current.scrollTop = 0;
    }
  }, [currentPage, scrollContainerRef]);
};

const MessageList = ({
  onConfigChange,
  config, pageSize, searchTypes, data: { id: searchTypeId, messages, total: totalMessages },
  setLoadingState,
  currentView: { activeQuery: activeQueryId },
  fields,
}: Props) => {
  const renderCompletionCallback = useContext(RenderCompletionCallback);
  const scrollContainerRef = useRef();
  const [{ currentPage, errors }, setState] = useState<State>({
    errors: [],
    currentPage: 1,
  });

  useResetScrollPositionOnPageChange(scrollContainerRef, currentPage);

  const _resetPagination = useCallback(() => {
    if (currentPage !== 1) {
      setState({ currentPage: 1, errors: [] });
    }
  }, [currentPage]);

  const _handlePageChange = useCallback((pageNo: number) => {
    // execute search with new offset
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

      setState({
        errors: response.result.errors,
        currentPage: pageNo,
      });
    });
  }, [pageSize, searchTypeId, searchTypes, setLoadingState]);

  const _onSortChange = useCallback((newSort: SortConfig[]) => {
    const newConfig = config.toBuilder().sort(newSort).build();

    return onConfigChange(newConfig).then(() => {});
  }, [config, onConfigChange]);

  useEffect(() => {
    InputsActions.list().then(() => (renderCompletionCallback && renderCompletionCallback()));
  }, [renderCompletionCallback]);

  useEffect(() => {
    const unlistenSearchExecute = SearchActions.execute.completed.listen(_resetPagination);

    return () => {
      unlistenSearchExecute();
    };
  }, [_resetPagination]);

  const hasError = !isEmpty(errors);

  return (
    <WindowDimensionsContextProvider>
      <Wrapper>
        <PaginatedList onChange={_handlePageChange}
                       activePage={Number(currentPage)}
                       showPageSizeSelect={false}
                       totalItems={totalMessages}
                       pageSize={pageSize}>
          {!hasError ? (
            <MessageTable activeQueryId={activeQueryId}
                          config={config}
                          scrollContainerRef={scrollContainerRef}
                          fields={fields}
                          onSortChange={_onSortChange}
                          setLoadingState={setLoadingState}
                          messages={messages} />
          ) : <ErrorWidget errors={errors} />}
        </PaginatedList>
      </Wrapper>
    </WindowDimensionsContextProvider>
  );
};

const mapProps = (props: {
  currentView: ViewStoreState,
  searches: SearchStoreState,
}) => ({
  currentView: props.currentView,
  searchTypes: get(props, ['searches', 'result', 'results', props.currentView.activeQuery, 'searchTypes']) as { [searchTypeId: string]: SearchType },
});

MessageList.defaultProps = {
  onConfigChange: () => Promise.resolve(Immutable.OrderedMap<string, Widget>()),
  pageSize: Messages.DEFAULT_LIMIT,
};

export default connect(MessageList, {
  currentView: ViewStore,
  searches: SearchStore,
}, mapProps);
