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
import React from 'react';

import { Alert } from 'components/bootstrap';
import { PaginatedEntityTable } from 'components/common';
import Routes from 'routing/Routes';
import Link from 'components/common/Link';
import AppConfig from 'util/AppConfig';

import { fetchIncompatibleIndices, incompatibleIndicesKeyFn } from './fetchIncompatibleIndices';
import type { IncompatibleIndexRow } from './fetchIncompatibleIndices';
import { createColumnRenderers, DEFAULT_DISPLAYED_COLUMNS } from './IncompatibleIndicesColumnRenderers';
import IncompatibleIndexTableActions from './IncompatibleIndexTableActions';
import IncompatibleIndicesBulkActions from './IncompatibleIndicesBulkActions';
import IncompatibleIndicesContext from './IncompatibleIndicesContext';
import useIncompatibleIndexActionState from './hooks/useIncompatibleIndexActionState';
import useTrackedIncompatibleIndices from './hooks/useTrackedIncompatibleIndices';

const TABLE_LAYOUT = {
  entityTableId: 'incompatible_indices',
  defaultSort: { attributeId: 'index_name', direction: 'asc' as const },
  defaultDisplayedAttributes: DEFAULT_DISPLAYED_COLUMNS,
  defaultPageSize: 10,
  defaultColumnOrder: ['index_name', 'category', 'version', 'begin', 'end'],
};

const DataNodeMigrationHint = () => {
  const migrationRouteAvailable =
    !AppConfig.isCloud() && AppConfig.isFeatureEnabled('data_node_migration');

  return migrationRouteAvailable ? (
    <Link to={Routes.SYSTEM.CLUSTER.DATANODE_MIGRATION}>Migrate to Data Nodes</Link>
  ) : (
    <>Migrate to Data Nodes</>
  );
};

type Props = {
  withoutURLParams?: boolean;
};

const IncompatibleIndicesTable = ({ withoutURLParams = false }: Props) => {
  const { selectedIndices, trackedIndices, hasLoaded, onDataLoaded, onChangeSelection } =
    useTrackedIncompatibleIndices();
  const contextValue = useIncompatibleIndexActionState({ trackedIndices, isLoading: !hasLoaded });
  const columnRenderers = createColumnRenderers();
  const renderActions = (index: IncompatibleIndexRow) => <IncompatibleIndexTableActions index={index} />;

  return (
    <IncompatibleIndicesContext.Provider value={contextValue}>
      {!contextValue.reindexActionsAvailable && (
        <Alert bsStyle="info">
          System indices can only be reindexed when Graylog runs against a Data Node search backend.{' '}
          <DataNodeMigrationHint /> to reindex them before upgrading to the next OpenSearch major version.
        </Alert>
      )}
      <PaginatedEntityTable<IncompatibleIndexRow>
        humanName="incompatible indices"
        tableLayout={TABLE_LAYOUT}
        fetchEntities={fetchIncompatibleIndices}
        keyFn={incompatibleIndicesKeyFn}
        columnRenderers={columnRenderers}
        entityActions={renderActions}
        bulkSelection={{ onChangeSelection, actions: <IncompatibleIndicesBulkActions indices={selectedIndices} /> }}
        onDataLoaded={onDataLoaded}
        entityAttributesAreCamelCase={false}
        withoutURLParams={withoutURLParams}
      />
    </IncompatibleIndicesContext.Provider>
  );
};

export default IncompatibleIndicesTable;
