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
import { get } from 'lodash';
import { useContext, useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';

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

type Pagination = {
  pageErrors: Array<{ description: string }>,
  currentPage: number
}

type SearchType = { effectiveTimerange: TimeRange };

export type MessageListResult = {
  messages: Array<BackendMessage>,
  total: number,
  id: string,
  type: 'messages'
};

type Props = WidgetComponentProps<MessagesWidgetConfig, MessageListResult> & {
  currentView: ViewStoreState,
  pageSize?: number,
  searchTypes: { [searchTypeId: string]: SearchType },
};

const useResetPaginationOnSearchExecution = (setPagination: (pagination: Pagination) => void, currentPage) => {
  useEffect(() => {
    const resetPagination = () => {
      if (currentPage !== 1) {
        setPagination({ currentPage: 1, pageErrors: [] });
      }
    };

    const unlistenSearchExecute = SearchActions.execute.completed.listen(resetPagination);

    return () => {
      unlistenSearchExecute();
    };
  }, [currentPage, setPagination]);
};

const useResetScrollPositionOnPageChange = (currentPage: number) => {
  const scrollContainerRef = useRef<HTMLDivElement>();

  useEffect(() => {
    if (scrollContainerRef.current) {
      // eslint-disable-next-line no-param-reassign
      scrollContainerRef.current.scrollTop = 0;
    }
  }, [currentPage, scrollContainerRef]);

  return scrollContainerRef;
};

const useRenderCompletionCallback = () => {
  const renderCompletionCallback = useContext(RenderCompletionCallback);

  useEffect(() => {
    InputsActions.list().then(() => (renderCompletionCallback && renderCompletionCallback()));
  }, [renderCompletionCallback]);
};

const MessageList = ({
  onConfigChange,
  config, pageSize, searchTypes, data: { id: searchTypeId, messages, total: totalMessages },
  setLoadingState,
  currentView: { activeQuery: activeQueryId },
  fields,
}: Props) => {
  const [{ currentPage, pageErrors }, setPagination] = useState<Pagination>({
    pageErrors: [],
    currentPage: 1,
  });

  const scrollContainerRef = useResetScrollPositionOnPageChange(currentPage);
  useResetPaginationOnSearchExecution(setPagination, currentPage);
  useRenderCompletionCallback();

  const handlePageChange = useCallback((pageNo: number) => {
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

      setPagination({
        pageErrors: response.result.errors,
        currentPage: pageNo,
      });
    });
  }, [pageSize, searchTypeId, searchTypes, setLoadingState]);

  const onSortChange = useCallback((newSort: SortConfig[]) => {
    const newConfig = config.toBuilder().sort(newSort).build();

    return onConfigChange(newConfig).then(() => {});
  }, [config, onConfigChange]);

  return (
    <WindowDimensionsContextProvider>
      <Wrapper>
        <PaginatedList onChange={handlePageChange}
                       activePage={Number(currentPage)}
                       showPageSizeSelect={false}
                       totalItems={totalMessages}
                       pageSize={pageSize}>
          {!pageErrors?.length ? (
            <MessageTable activeQueryId={activeQueryId}
                          config={config}
                          scrollContainerRef={scrollContainerRef}
                          fields={fields}
                          onSortChange={onSortChange}
                          setLoadingState={setLoadingState}
                          messages={messages} />
          ) : <ErrorWidget errors={pageErrors} />}
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

MessageList.propTypes = {
  onConfigChange: PropTypes.func,
  pageSize: PropTypes.number,
};

MessageList.defaultProps = {
  onConfigChange: () => Promise.resolve(Immutable.OrderedMap<string, Widget>()),
  pageSize: Messages.DEFAULT_LIMIT,
};

export default connect(MessageList, {
  currentView: ViewStore,
  searches: SearchStore,
}, mapProps);
