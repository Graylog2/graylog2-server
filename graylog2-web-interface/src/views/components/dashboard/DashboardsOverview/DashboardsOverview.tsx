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
import CurrentUser from 'logic/users/CurrentUser';
import type { SearchParamsForDashboards } from 'views/components/dashboard/SearchParamsForDashboards';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { getVisibleAttributeColumns } from 'components/common/EntityDataTable/hooks/useVisibleColumnOrder';
import useSharedWithOthersSection from 'components/permissions/SharedWithOthers/useSharedWithOthersSection';
import { SHARED_WITH_OTHERS_ID } from 'components/permissions/SharedWithOthers/Constants';

import BulkActions from './BulkActions';

type Props = {
  hideShare?: boolean;
  hideAdditionalColumns?: boolean;
  hideDelete?: boolean;
};

const DashboardsOverview = ({ hideAdditionalColumns = false, hideShare = false, hideDelete = false }: Props) => {
  const { pluggableColumnRenderers, pluggableAttributes, pluggableExpandedSections } =
    usePluggableEntityTableElements<View>(null, 'dashboard');
  const { getDefaultLayout, additionalAttributes } = getDashboardTableElements(pluggableAttributes);
  const customColumnRenderers = useColumnRenderers(pluggableColumnRenderers);
  const tableLayout = getDefaultLayout(hideAdditionalColumns);
  const { data: layoutPreferences } = useUserLayoutPreferences(tableLayout.entityTableId);
  const visibleColumns = getVisibleAttributeColumns(
    tableLayout.defaultDisplayedAttributes,
    layoutPreferences?.attributes,
  );
  const sharedWithOthersSection = useSharedWithOthersSection<View>(
    'dashboard',
    'dashboard',
    visibleColumns.has(SHARED_WITH_OTHERS_ID),
  );

  const renderDashboardActions = useCallback(
    (dashboard: View) => <DashboardActions dashboard={dashboard} hideDelete={hideDelete} hideShare={hideShare} />,
    [hideDelete, hideShare],
  );
  const expandedSections = useMemo(
    () => ({
      ...pluggableExpandedSections,
      ...sharedWithOthersSection,
    }),
    [pluggableExpandedSections, sharedWithOthersSection],
  );

  const fetchEntities = (searchParams: SearchParamsForDashboards): Promise<PaginatedResponse<View>> => {
    CurrentUser.reload();

    return fetchDashboards(searchParams);
  };

  return (
    <PaginatedEntityTable<View>
      humanName="dashboards"
      queryHelpComponent={
        <QueryHelper entityName="dashboard" commonFields={['id', 'title', 'description', 'summary']} />
      }
      entityActions={renderDashboardActions}
      tableLayout={tableLayout}
      fetchEntities={fetchEntities}
      additionalAttributes={additionalAttributes}
      expandedSectionRenderers={expandedSections}
      keyFn={keyFn}
      entityAttributesAreCamelCase
      bulkSelection={{ actions: <BulkActions /> }}
      columnRenderers={customColumnRenderers}
    />
  );
};

export default DashboardsOverview;
