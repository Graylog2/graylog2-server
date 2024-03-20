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
import { useCallback, useState } from 'react';
import styled from 'styled-components';

import type { WidgetComponentProps } from 'views/types';
import { PaginatedList } from 'components/common';
import type { SearchTypeOptions } from 'views/logic/search/GlobalOverride';
import reexecuteSearchTypes from 'views/components/widgets/reexecuteSearchTypes';
import useAppDispatch from 'stores/useAppDispatch';
import type EventsWidgetConfig from 'views/logic/widgets/events/EventsWidgetConfig';
import type { EventsListResult } from 'views/components/widgets/events/types';
import type EventsWidgetSortConfig from 'views/logic/widgets/events/EventsWidgetSortConfig';

import EventsTable from './EventsTable';

import { PAGINATION } from '../Constants';

const Container = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: auto;
  height: 100%;
`;

const useRefreshPage = (searchTypeId: string, setLoadingState: React.Dispatch<React.SetStateAction<boolean>>) => {
  const dispatch = useAppDispatch();

  return useCallback((page: number, perPage: number) => {
    const searchTypePayload: SearchTypeOptions<{
      page: number,
      per_page: number,
    }> = {
      [searchTypeId]: {
        page,
        per_page: perPage,
      },
    };

    setLoadingState(true);

    return dispatch(reexecuteSearchTypes(searchTypePayload)).then(() => {
      setLoadingState(false);
    });
  }, [dispatch, searchTypeId, setLoadingState]);
};

const EventsList = ({ data, config, onConfigChange, setLoadingState }: WidgetComponentProps<EventsWidgetConfig, EventsListResult>) => {
  const [currentPage, setCurrentPage] = useState(PAGINATION.INITIAL_PAGE);
  const refreshPage = useRefreshPage(data.id, setLoadingState);

  const handlePageChange = useCallback((newPage: number, newPageSize: number) => {
    refreshPage(newPage, newPageSize).then(() => {
      setCurrentPage(newPage);
    });
  }, [refreshPage]);

  const onSortChange = useCallback((newSort: EventsWidgetSortConfig) => {
    const newConfig = config.toBuilder().sort(newSort).build();

    return onConfigChange(newConfig);
  }, [config, onConfigChange]);

  return (
    <Container>
      <PaginatedList onChange={handlePageChange}
                     useQueryParameter={false}
                     activePage={currentPage}
                     pageSize={PAGINATION.PER_PAGE}
                     showPageSizeSelect={false}
                     totalItems={data.totalResults ?? 0}>
        <EventsTable config={config}
                     setLoadingState={setLoadingState}
                     onSortChange={onSortChange}
                     events={data.events} />
      </PaginatedList>
    </Container>
  );
};

export default EventsList;
