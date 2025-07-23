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

import BulkActions from './BulkActions';

type Props = {
  isEvidenceModal?: boolean;
};

const DashboardsOverview = ({ isEvidenceModal = false }: Props) => {
  const { pluggableColumnRenderers, pluggableAttributes, pluggableExpandedSections } =
    usePluggableEntityTableElements<View>(null, 'dashboard');
  const { getDefaultLayout, columnOrder, additionalAttributes } = getDashboardTableElements(pluggableAttributes);
  const customColumnRenderers = useColumnRenderers(pluggableColumnRenderers);

  const renderDashboardActions = useCallback(
    (dashboard: View) => <DashboardActions dashboard={dashboard} isEvidenceModal={isEvidenceModal} />,
    [isEvidenceModal],
  );
  const expandedSections = useMemo(
    () => ({
      ...pluggableExpandedSections,
    }),
    [pluggableExpandedSections],
  );

  return (
    <PaginatedEntityTable<View>
      humanName="dashboards"
      columnsOrder={columnOrder}
      queryHelpComponent={
        <QueryHelper entityName="dashboard" commonFields={['id', 'title', 'description', 'summary']} />
      }
      entityActions={renderDashboardActions}
      tableLayout={getDefaultLayout(isEvidenceModal)}
      fetchEntities={fetchDashboards}
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
