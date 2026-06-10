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
import { useState, useCallback, useMemo, useEffect } from 'react';
import cloneDeep from 'lodash/cloneDeep';
import find from 'lodash/find';
import isEmpty from 'lodash/isEmpty';
import debounce from 'lodash/debounce';

import { naturalSortIgnoreCase } from 'util/SortUtils';
import { Spinner } from 'components/common';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import {
  CollectorConfigurationsActions,
  CollectorConfigurationsStore,
} from 'stores/sidecars/CollectorConfigurationsStore';
import { CollectorsActions, CollectorsStore } from 'stores/sidecars/CollectorsStore';
import { SidecarsActions } from 'stores/sidecars/SidecarsStore';
import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';
import { useStore } from 'stores/connect';
import type { SidecarCollectorPairType, Configuration, Collector } from 'components/sidecars/types';
import useSidecarsAdministration, { useSetSidecarAction } from 'hooks/useSidecarsAdministration';

import CollectorsAdministration, { PAGE_SIZES } from './CollectorsAdministration';

type Props = {
  nodeId?: string;
  paginationQueryParameter: PaginationQueryParameterResult;
};

const SEARCH_DEBOUNCE_THRESHOLD = 500;

const CollectorsAdministrationContainer = ({ ...props }: Props) => {
  const collectors = useStore(CollectorsStore);
  const configurations = useStore(CollectorConfigurationsStore);

  const { page: initialPage, pageSize: initialPageSize } = props.paginationQueryParameter;
  const initialQuery = props.nodeId ? `node_id:${props.nodeId}` : '';

  const [query, setQuery] = useState(initialQuery);
  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [filters, setFilters] = useState<{}>({});

  const { data: sidecars } = useSidecarsAdministration({ query, page, pageSize, filters });
  const { mutateAsync: setAction } = useSetSidecarAction();

  useEffect(() => {
    CollectorsActions.all();
    CollectorConfigurationsActions.all();
  }, []);

  const handlePageChange = useCallback((newPage: number, newPageSize: number) => {
    setPage(newPage);
    setPageSize(newPageSize);
  }, []);

  const handleFilter = useCallback(
    (property: string, value: string) => {
      const { resetPage } = props.paginationQueryParameter;

      if (property) {
        setFilters((prev) => {
          const newFilters = cloneDeep(prev);
          newFilters[property] = value;

          return newFilters;
        });
      } else {
        setFilters({});
      }

      resetPage();
      setPage(1);
    },
    [props.paginationQueryParameter],
  );

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const handleQueryChange = useCallback(
    debounce((newQuery = '', callback = () => {}) => {
      setQuery(newQuery);
      setPage(1);
      callback();
    }, SEARCH_DEBOUNCE_THRESHOLD),
    [],
  );

  const handleConfigurationChange = useCallback(
    (
      selectedSidecars: SidecarCollectorPairType[],
      selectedConfigurations: Configuration[],
      doneCallback: () => void,
    ) => {
      SidecarsActions.assignConfigurations(selectedSidecars, selectedConfigurations).then((response) => {
        doneCallback();

        return response;
      });
    },
    [],
  );

  const handleProcessAction = useCallback(
    (action: string, selectedCollectors: { [sidecarId: string]: string[] }, doneCallback: () => void) => {
      setAction({ action, collectors: selectedCollectors }).then((response) => {
        doneCallback();

        return response;
      });
    },
    [setAction],
  );

  const sidecarCollectors = useMemo(() => {
    if (!collectors?.collectors || !sidecars?.sidecars || !configurations?.configurations) {
      return null;
    }

    const result: SidecarCollectorPairType[] = [];

    sidecars.sidecars
      .sort((s1, s2) => naturalSortIgnoreCase(s1.node_name, s2.node_name))
      .forEach((sidecar) => {
        const compatibleCollectorIds = sidecar.collectors;

        if (isEmpty(compatibleCollectorIds)) {
          result.push({ collector: {} as Collector, sidecar: sidecar });

          return;
        }

        compatibleCollectorIds
          .map((id) => find(collectors.collectors, { id: id }))
          .forEach((compatibleCollector) => {
            result.push({ collector: compatibleCollector, sidecar: sidecar });
          });
      });

    return result;
  }, [collectors?.collectors, sidecars?.sidecars, configurations?.configurations]);

  if (!sidecarCollectors) {
    return <Spinner text="Loading collector list..." />;
  }

  return (
    <CollectorsAdministration
      sidecarCollectorPairs={sidecarCollectors}
      collectors={collectors.collectors}
      configurations={configurations.configurations}
      pagination={sidecars.pagination}
      query={sidecars.query}
      filters={sidecars.filters}
      onPageChange={handlePageChange}
      onFilter={handleFilter}
      onQueryChange={handleQueryChange}
      onConfigurationChange={handleConfigurationChange}
      onProcessAction={handleProcessAction}
    />
  );
};

export default withPaginationQueryParameter(CollectorsAdministrationContainer, { pageSizes: PAGE_SIZES });
