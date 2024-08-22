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
import { useCallback, useContext, useEffect, useState } from 'react';
import styled from 'styled-components';

import type { WidgetComponentProps } from 'views/types';
import { PaginatedList } from 'components/common';
import type { SearchTypeOptions } from 'views/logic/search/GlobalOverride';
import reexecuteSearchTypes from 'views/components/widgets/reexecuteSearchTypes';
import useAppDispatch from 'stores/useAppDispatch';
import type EventsWidgetConfig from 'views/logic/widgets/events/EventsWidgetConfig';
import type { EventsListResult } from 'views/components/widgets/events/types';
import type EventsWidgetSortConfig from 'views/logic/widgets/events/EventsWidgetSortConfig';
import useOnSearchExecution from 'views/hooks/useOnSearchExecution';
import useAutoRefresh from 'views/hooks/useAutoRefresh';
import ErrorWidget from 'views/components/widgets/ErrorWidget';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';

import EventsTable from './EventsTable';

import { PAGINATION } from '../Constants';

type Pagination = {
  pageErrors: Array<{ description: string }>,
  currentPage: number
}

const Container = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: auto;
  height: 100%;
`;

const useResetPaginationOnSearchExecution = (setPagination: (pagination: Pagination) => void, currentPage) => {
  const resetPagination = useCallback(() => {
    if (currentPage !== 1) {
      setPagination({ currentPage: 1, pageErrors: [] });
    }
  }, [currentPage, setPagination]);
  useOnSearchExecution(resetPagination);
};

const useHandlePageChange = (searchTypeId: string, setLoadingState: (loading: boolean) => void, setPagination: (pagination: Pagination) => void) => {
  const dispatch = useAppDispatch();
  const { stopAutoRefresh } = useAutoRefresh();

  return useCallback((pageNo: number) => {
    // execute search with new offset
    const searchTypePayload: SearchTypeOptions<{
      page: number,
      per_page: number,
    }> = {
      [searchTypeId]: {
        page: pageNo,
        per_page: PAGINATION.PER_PAGE,
      },
    };

    stopAutoRefresh();
    setLoadingState(true);

    return dispatch(reexecuteSearchTypes(searchTypePayload)).then((response) => {
      const { result } = response.payload;
      setLoadingState(false);

      setPagination({
        pageErrors: result.errors,
        currentPage: pageNo,
      });
    });
  }, [dispatch, searchTypeId, setLoadingState, setPagination, stopAutoRefresh]);
};

const EventsList = ({ data, config, onConfigChange, setLoadingState }: WidgetComponentProps<EventsWidgetConfig, EventsListResult>) => {
  const [{ currentPage, pageErrors }, setPagination] = useState<Pagination>({
    pageErrors: [],
    currentPage: PAGINATION.INITIAL_PAGE,
  });

  useResetPaginationOnSearchExecution(setPagination, currentPage);

  const handlePageChange = useHandlePageChange(data.id, setLoadingState, setPagination);

  const onSortChange = useCallback((newSort: EventsWidgetSortConfig) => {
    const newConfig = config.toBuilder().sort(newSort).build();

    return onConfigChange(newConfig);
  }, [config, onConfigChange]);
  const onRenderComplete = useContext(RenderCompletionCallback);

  useEffect(() => {
    onRenderComplete();
  }, [onRenderComplete]);

  return (
    <Container>
      <PaginatedList onChange={handlePageChange}
                     useQueryParameter={false}
                     activePage={currentPage}
                     pageSize={PAGINATION.PER_PAGE}
                     showPageSizeSelect={false}
                     totalItems={data.totalResults ?? 0}>
        {!pageErrors?.length ? (
          <EventsTable config={config}
                       setLoadingState={setLoadingState}
                       onSortChange={onSortChange}
                       events={data.events} />
        ) : <ErrorWidget errors={pageErrors} />}
      </PaginatedList>
    </Container>
  );
};

export default EventsList;
