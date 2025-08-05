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
import styled from 'styled-components';
import { useContext, useState, useEffect, useCallback, useRef } from 'react';

import type { WidgetComponentProps, MessageResult } from 'views/types';
import { Messages } from 'views/Constants';
import type MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import type { SearchTypeOptions } from 'views/logic/search/GlobalOverride';
import { PaginatedList } from 'components/common';
import MessageTable from 'views/components/widgets/MessageTable';
import ErrorWidget from 'views/components/widgets/ErrorWidget';
import type SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import type { BackendMessage } from 'views/components/messagelist/Types';
import WindowDimensionsContextProvider from 'contexts/WindowDimensionsContextProvider';
import { InputsActions } from 'stores/inputs/InputsStore';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import useCurrentSearchTypesResults from 'views/components/widgets/useCurrentSearchTypesResults';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import reexecuteSearchTypes from 'views/components/widgets/reexecuteSearchTypes';
import useOnSearchExecution from 'views/hooks/useOnSearchExecution';
import useAutoRefresh from 'views/hooks/useAutoRefresh';
import useSearchResult from 'views/hooks/useSearchResult';

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

export type MessageListResult = {
  messages: Array<BackendMessage>;
  total: number;
  id: string;
  type: 'messages';
};

type Props = WidgetComponentProps<MessagesWidgetConfig, MessageListResult> & {
  pageSize?: number;
};

const useResetPaginationOnSearchExecution = (setCurrentPage: (pageNr: number) => void, currentPage) => {
  const resetPagination = useCallback(() => {
    if (currentPage !== 1) {
      setCurrentPage(1);
    }
  }, [currentPage, setCurrentPage]);
  useOnSearchExecution(resetPagination);
};

const useResetScrollPositionOnPageChange = (currentPage: number) => {
  const scrollContainerRef = useRef<HTMLDivElement>();

  useEffect(() => {
    if (scrollContainerRef.current) {
      scrollContainerRef.current.scrollTop = 0;
    }
  }, [currentPage, scrollContainerRef]);

  return scrollContainerRef;
};

const useRenderCompletionCallback = () => {
  const renderCompletionCallback = useContext(RenderCompletionCallback);

  useEffect(() => {
    InputsActions.list().then(() => renderCompletionCallback && renderCompletionCallback());
  }, [renderCompletionCallback]);
};

const usePageErrors = (searchTypeId: string) => {
  const searchResult = useSearchResult();
  const errors = searchResult?.result?.errors ?? [];

  return errors.filter((error) => error.searchTypeId === searchTypeId);
};

const MessageList = ({
  config,
  data: { id: searchTypeId, messages, total: totalMessages },
  fields,
  onConfigChange = () => Promise.resolve(),
  pageSize = Messages.DEFAULT_LIMIT,
  setLoadingState,
}: Props) => {
  const [currentPage, setCurrentPage] = useState<number>(1);
  const { stopAutoRefresh } = useAutoRefresh();

  const pageErrors = usePageErrors(searchTypeId);
  const activeQueryId = useActiveQueryId();
  const searchTypes = useCurrentSearchTypesResults();
  const scrollContainerRef = useResetScrollPositionOnPageChange(currentPage);
  const dispatch = useViewsDispatch();
  useResetPaginationOnSearchExecution(setCurrentPage, currentPage);
  useRenderCompletionCallback();

  const handlePageChange = useCallback(
    (newCurrentPage: number) => {
      // execute search with new offset
      const { effectiveTimerange } = searchTypes[searchTypeId] as MessageResult;
      const searchTypePayload: SearchTypeOptions<{
        limit: number;
        offset: number;
      }> = {
        [searchTypeId]: {
          limit: pageSize,
          offset: pageSize * (newCurrentPage - 1),
        },
      };

      stopAutoRefresh();
      setLoadingState(true);
      setCurrentPage(newCurrentPage);

      dispatch(reexecuteSearchTypes(searchTypePayload, effectiveTimerange)).then(() => {
        setLoadingState(false);
      });
    },
    [dispatch, pageSize, searchTypeId, searchTypes, setLoadingState, stopAutoRefresh],
  );

  const onSortChange = useCallback(
    (newSort: SortConfig[]) => {
      const newConfig = config.toBuilder().sort(newSort).build();

      return onConfigChange(newConfig);
    },
    [config, onConfigChange],
  );

  return (
    <WindowDimensionsContextProvider>
      <Wrapper>
        <PaginatedList
          onChange={handlePageChange}
          activePage={Number(currentPage)}
          showPageSizeSelect={false}
          totalItems={totalMessages}
          pageSize={pageSize}
          enforcePageBounds={false}
          useQueryParameter={false}>
          {pageErrors.length > 0 ? (
            <ErrorWidget errors={pageErrors} />
          ) : (
            <MessageTable
              activeQueryId={activeQueryId}
              config={config}
              scrollContainerRef={scrollContainerRef}
              fields={fields}
              onSortChange={onSortChange}
              setLoadingState={setLoadingState}
              messages={messages}
            />
          )}
        </PaginatedList>
      </Wrapper>
    </WindowDimensionsContextProvider>
  );
};

export default MessageList;
