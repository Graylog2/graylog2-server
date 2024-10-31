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
import { useCallback } from 'react';

import type { Sort } from 'stores/PaginationTypes';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';
import type { TableLayoutPreferences } from 'components/common/EntityDataTable/types';

const useTableEventHandlers = ({
  updateTableLayout,
  paginationQueryParameter,
  setQuery,
  appSection,
}: {
  updateTableLayout: (preferences: TableLayoutPreferences) => void,
  paginationQueryParameter: PaginationQueryParameterResult,
  setQuery: (query: string) => void,
  appSection: string
}) => {
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  const onPageSizeChange = useCallback((newPageSize: number) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_LIST.PAGE_SIZE_CHANGED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: appSection,
      app_action_value: 'page-size-select',
      page_size: newPageSize,
    });

    paginationQueryParameter.setPagination({ page: 1, pageSize: newPageSize });
    updateTableLayout({ perPage: newPageSize });
  }, [appSection, paginationQueryParameter, pathname, sendTelemetry, updateTableLayout]);

  const onSearch = useCallback((newQuery: string) => {
    paginationQueryParameter.resetPage();
    setQuery(newQuery);
  }, [paginationQueryParameter, setQuery]);

  const onSearchReset = useCallback(() => {
    onSearch('');
  }, [onSearch]);

  const onColumnsChange = useCallback((displayedAttributes: Array<string>) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_LIST.COLUMNS_CHANGED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: appSection,
      app_action_value: 'columns-select',
      columns: displayedAttributes,
    });

    updateTableLayout({ displayedAttributes });
  }, [appSection, pathname, sendTelemetry, updateTableLayout]);

  const onSortChange = useCallback((newSort: Sort) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_LIST.SORT_CHANGED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: appSection,
      app_action_value: 'sort-select',
      sort: newSort,
    });

    paginationQueryParameter.resetPage();
    updateTableLayout({ sort: newSort });
  }, [appSection, paginationQueryParameter, pathname, sendTelemetry, updateTableLayout]);

  return {
    onPageSizeChange,
    onSearch,
    onSearchReset,
    onColumnsChange,
    onSortChange,
  };
};

export default useTableEventHandlers;
