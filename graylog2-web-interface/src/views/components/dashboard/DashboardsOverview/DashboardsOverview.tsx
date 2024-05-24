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
import React, { useCallback } from 'react';

import QueryHelper from 'components/common/QueryHelper';
import type View from 'views/logic/views/View';
import { fetchDashboards, keyFn } from 'views/components/dashboard/hooks/useDashboards';
import DashboardActions from 'views/components/dashboard/DashboardsOverview/DashboardActions';
import useColumnRenderers from 'views/components/dashboard/DashboardsOverview/useColumnRenderers';
import { DEFAULT_LAYOUT, COLUMNS_ORDER } from 'views/components/dashboard/DashboardsOverview/Constants';
import PageEntityTable from 'components/common/PageEntityTable';

import BulkActions from './BulkActions';

type Props = {
  isEvidenceModal?: boolean,
};

const DashboardsOverview = ({ isEvidenceModal }: Props) => {
  const customColumnRenderers = useColumnRenderers();

  const renderDashboardActions = useCallback((dashboard: View) => (
    <DashboardActions dashboard={dashboard} refetchDashboards={() => {}} isEvidenceModal={isEvidenceModal} />
  ), [isEvidenceModal]);

  return (
    <PageEntityTable<View> humanName="dashboards"
                           columnsOrder={COLUMNS_ORDER}
                           queryHelpComponent={<QueryHelper entityName="dashboard" commonFields={['id', 'title', 'description', 'summary']} />}
                           entityActions={renderDashboardActions}
                           tableLayout={DEFAULT_LAYOUT(isEvidenceModal)}
                           fetchEntities={fetchDashboards}
                           keyFn={keyFn}
                           bulkSelection={{ actions: <BulkActions /> }}
                           columnRenderers={customColumnRenderers} />
  );
};

DashboardsOverview.defaultProps = {
  isEvidenceModal: false,
};

export default DashboardsOverview;
