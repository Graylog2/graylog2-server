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
import React, { useCallback, useMemo } from 'react';

import QueryHelper from 'components/common/QueryHelper';
import type View from 'views/logic/views/View';
import { fetchDashboards, keyFn } from 'views/components/dashboard/hooks/useDashboards';
import DashboardActions from 'views/components/dashboard/DashboardsOverview/DashboardActions';
import useColumnRenderers from 'views/components/dashboard/DashboardsOverview/useColumnRenderers';
import getDashboardTableElements from 'views/components/dashboard/DashboardsOverview/Constants';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import usePluggableEntityTableElements from 'hooks/usePluggableEntityTableElements';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import type { SearchParamsForDashboards } from 'views/components/dashboard/SearchParamsForDashboards';

import BulkActions from './BulkActions';

type Props = {
  hideShare?: boolean;
  hideAdditionalColumns?: boolean;
  hideDelete?: boolean;
};

const DashboardsOverview = ({ hideAdditionalColumns = false, hideShare = false, hideDelete = false }: Props) => {
  const { pluggableColumnRenderers, pluggableAttributes, pluggableExpandedSections } =
    usePluggableEntityTableElements<View>(null, 'dashboard');
  const { getDefaultLayout, columnOrder, additionalAttributes } = getDashboardTableElements(pluggableAttributes);
  const customColumnRenderers = useColumnRenderers(pluggableColumnRenderers);

  const renderDashboardActions = useCallback(
    (dashboard: View) => <DashboardActions dashboard={dashboard} hideDelete={hideDelete} hideShare={hideShare} />,
    [hideDelete, hideShare],
  );
  const expandedSections = useMemo(
    () => ({
      ...pluggableExpandedSections,
    }),
    [pluggableExpandedSections],
  );

  const fetchEntities = (searchParams: SearchParamsForDashboards): Promise<PaginatedResponse<View>> => {
    CurrentUserStore.update(CurrentUserStore.getInitialState().currentUser.username);

    return fetchDashboards(searchParams);
  };

  return (
    <PaginatedEntityTable<View>
      humanName="dashboards"
      columnsOrder={columnOrder}
      queryHelpComponent={
        <QueryHelper entityName="dashboard" commonFields={['id', 'title', 'description', 'summary']} />
      }
      entityActions={renderDashboardActions}
      tableLayout={getDefaultLayout(hideAdditionalColumns)}
      fetchEntities={fetchEntities}
      additionalAttributes={additionalAttributes}
      expandedSectionsRenderer={expandedSections}
      keyFn={keyFn}
      entityAttributesAreCamelCase
      bulkSelection={{ actions: <BulkActions /> }}
      columnRenderers={customColumnRenderers}
    />
  );
};

export default DashboardsOverview;
