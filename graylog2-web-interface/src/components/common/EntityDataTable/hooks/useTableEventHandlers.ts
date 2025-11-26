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
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';
import type { TableLayoutPreferences, ColumnPreferences } from 'components/common/EntityDataTable/types';
import { ATTRIBUTE_STATUS } from 'components/common/EntityDataTable/Constants';

const useTableEventHandlers = ({
  updateTableLayout,
  paginationQueryParameter,
  setQuery,
  appSection,
}: {
  updateTableLayout: (preferences: TableLayoutPreferences) => Promise<void>;
  paginationQueryParameter: PaginationQueryParameterResult;
  setQuery: (query: string) => void;
  appSection: string;
}) => {
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  const onPageSizeChange = useCallback(
    (newPageSize: number) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.PAGE_SIZE_CHANGED, {
        app_section: appSection,
        app_action_value: 'page-size-select',
        page_size: newPageSize,
      });

      paginationQueryParameter.setPagination({ page: 1, pageSize: newPageSize });
      updateTableLayout({ perPage: newPageSize });
    },
    [appSection, paginationQueryParameter, pathname, sendTelemetry, updateTableLayout],
  );

  const onSearch = useCallback(
    (newQuery: string) => {
      paginationQueryParameter.resetPage();
      setQuery(newQuery);
    },
    [paginationQueryParameter, setQuery],
  );

  const onLayoutPreferencesChange = useCallback(
    (layoutPreferences: { attributes?: ColumnPreferences; order?: Array<string> }) => {
      if (layoutPreferences.order) {
        sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.COLUMN_ORDER_CHANGED, {
          app_section: appSection,
          app_action_value: 'column-order-change',
          column_order: layoutPreferences.order,
        });
      }

      if (layoutPreferences.attributes) {
        sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.COLUMNS_CHANGED, {
          app_section: appSection,
          app_action_value: 'columns-select',
          columns: Object.keys(layoutPreferences.attributes).filter(
            (key) => layoutPreferences.attributes[key].status === ATTRIBUTE_STATUS.show,
          ),
        });
      }

      if (layoutPreferences.attributes === null && layoutPreferences.order === null) {
        sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.COLUMNS_RESET, {
          app_section: appSection,
          app_action_value: 'columns-select',
        });
      }

      const newLayoutPreferences: { attributes?: ColumnPreferences; order?: Array<string> } = {};

      if ('order' in layoutPreferences) {
        newLayoutPreferences.order = layoutPreferences.order;
      }

      if ('attributes' in layoutPreferences) {
        newLayoutPreferences.attributes = layoutPreferences.attributes;
      }

      return updateTableLayout(newLayoutPreferences);
    },
    [appSection, pathname, sendTelemetry, updateTableLayout],
  );

  const onSearchReset = useCallback(() => {
    onSearch('');
  }, [onSearch]);

  const onSortChange = useCallback(
    (newSort: Sort) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SORT_CHANGED, {
        app_section: appSection,
        app_action_value: 'sort-select',
        sort: newSort,
      });

      paginationQueryParameter.resetPage();
      updateTableLayout({ sort: newSort });
    },
    [appSection, paginationQueryParameter, pathname, sendTelemetry, updateTableLayout],
  );

  return {
    onLayoutPreferencesChange,
    onPageSizeChange,
    onSearch,
    onSearchReset,
    onSortChange,
  };
};

export default useTableEventHandlers;
