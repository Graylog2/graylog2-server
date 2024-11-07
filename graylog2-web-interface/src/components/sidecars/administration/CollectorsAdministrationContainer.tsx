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
import { useEffect } from 'react';
import cloneDeep from 'lodash/cloneDeep';
import find from 'lodash/find';
import isEmpty from 'lodash/isEmpty';

import { naturalSortIgnoreCase } from 'util/SortUtils';
import { Spinner } from 'components/common';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import { CollectorConfigurationsActions, CollectorConfigurationsStore } from 'stores/sidecars/CollectorConfigurationsStore';
import { CollectorsActions, CollectorsStore } from 'stores/sidecars/CollectorsStore';
import { SidecarsActions } from 'stores/sidecars/SidecarsStore';
import { SidecarsAdministrationActions, SidecarsAdministrationStore } from 'stores/sidecars/SidecarsAdministrationStore';
import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';
import { useStore } from 'stores/connect';
import type { SidecarCollectorPairType, Configuration } from 'components/sidecars/types';

import CollectorsAdministration, { PAGE_SIZES } from './CollectorsAdministration';

type Props = {
  nodeId?: string,
  paginationQueryParameter: PaginationQueryParameterResult,
}

const CollectorsAdministrationContainer = (props: Props) => {
  const collectors = useStore(CollectorsStore);
  const sidecars = useStore(SidecarsAdministrationStore);
  const configurations = useStore(CollectorConfigurationsStore);

  const reloadSidecars = () => {
    if (sidecars) {
      SidecarsAdministrationActions.refreshList();
    }
  };

  const loadData = (nodeId: string) => {
    const { page, pageSize } = props.paginationQueryParameter;
    const query = nodeId ? `node_id:${nodeId}` : '';

    CollectorsActions.all();
    SidecarsAdministrationActions.list({ query, page, pageSize });
    CollectorConfigurationsActions.all();
  };

  useEffect(() => {
    loadData(props.nodeId);
  }, [props?.nodeId]);

  useEffect(() => {
    const interval = setInterval(reloadSidecars, 5000);

    return () => clearInterval(interval);
  }, []);

  const handlePageChange = (page: number, pageSize: number) => {
    const { filters, query } = sidecars;

    SidecarsAdministrationActions.list({ query, filters, page, pageSize });
  };

  const handleFilter = (property: string, value: string) => {
    const { resetPage, pageSize } = props.paginationQueryParameter;
    const { filters, query } = sidecars;
    let newFilters;

    if (property) {
      newFilters = cloneDeep(filters);
      newFilters[property] = value;
    } else {
      newFilters = {};
    }

    resetPage();

    SidecarsAdministrationActions.list({ query, filters: newFilters, pageSize, page: 1 });
  };

  const handleQueryChange = (query = '', callback = () => {}) => {
    const { resetPage, pageSize } = props.paginationQueryParameter;
    const { filters } = sidecars;

    resetPage();

    SidecarsAdministrationActions.list({ query, filters, pageSize, page: 1 }).finally(callback);
  };

  const handleConfigurationChange = (selectedSidecars: SidecarCollectorPairType[], selectedConfigurations: Configuration[], doneCallback: () => void) => {
    SidecarsActions.assignConfigurations(selectedSidecars, selectedConfigurations).then((response) => {
      doneCallback();
      const { query, filters } = sidecars;
      const { page, pageSize } = props.paginationQueryParameter;

      SidecarsAdministrationActions.list({ query, filters, pageSize, page });

      return response;
    });
  };

  const handleProcessAction = (action: string, selectedCollectors: { [sidecarId: string]: string[] }, doneCallback: () => void) => {
    SidecarsAdministrationActions.setAction(action, selectedCollectors).then((response) => {
      doneCallback();

      return response;
    });
  };

  if (!collectors?.collectors || !sidecars?.sidecars || !configurations?.configurations) {
    return <Spinner text="Loading collector list..." />;
  }

  const sidecarCollectors = [];

  sidecars.sidecars
    .sort((s1, s2) => naturalSortIgnoreCase(s1.node_name, s2.node_name))
    .forEach((sidecar) => {
      const compatibleCollectorIds = sidecar.collectors;

      if (isEmpty(compatibleCollectorIds)) {
        sidecarCollectors.push({ collector: {}, sidecar: sidecar });

        return;
      }

      compatibleCollectorIds
        .map((id) => find(collectors.collectors, { id: id }))
        .forEach((compatibleCollector) => {
          sidecarCollectors.push({ collector: compatibleCollector, sidecar: sidecar });
        });
    });

  return (
    <CollectorsAdministration sidecarCollectorPairs={sidecarCollectors}
                              collectors={collectors.collectors}
                              configurations={configurations.configurations}
                              pagination={sidecars.pagination}
                              query={sidecars.query}
                              filters={sidecars.filters}
                              onPageChange={handlePageChange}
                              onFilter={handleFilter}
                              onQueryChange={handleQueryChange}
                              onConfigurationChange={handleConfigurationChange}
                              onProcessAction={handleProcessAction} />
  );
};

export default withPaginationQueryParameter(CollectorsAdministrationContainer, { pageSizes: PAGE_SIZES });
