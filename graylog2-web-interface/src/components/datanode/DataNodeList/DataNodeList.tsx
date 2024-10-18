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

import type { DataNode, DataNodeStatus } from 'preflight/types';
import { RelativeTime, PaginatedEntityTable } from 'components/common';
import QueryHelper from 'components/common/QueryHelper';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import type { Sort } from 'stores/PaginationTypes';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

import DataNodeActions from './DataNodeActions';
import DataNodeStatusCell from './DataNodeStatusCell';
import DataNodeBulkActions from './DataNodeBulkActions';

import { fetchDataNodes, keyFn } from '../hooks/useDataNodes';

const DEFAULT_LAYOUT = {
  entityTableId: 'datanodes',
  defaultPageSize: 10,
  defaultSort: { attributeId: 'hostname', direction: 'asc' } as Sort,
  defaultDisplayedAttributes: ['hostname', 'transport_address', 'data_node_status', 'is_leader', 'cert_valid_until', 'datanode_version'],
};

const COLUMNS_ORDER = ['hostname', 'transport_address', 'data_node_status', 'is_leader', 'cert_valid_until', 'datanode_version'];

const columnRenderers: ColumnRenderers<DataNode> = {
  attributes: {
    hostname: {
      renderCell: (_hostname: string, dataNode: DataNode) => (
        <Link to={Routes.SYSTEM.DATANODES.SHOW(dataNode.node_id)}>
          {dataNode.hostname}
        </Link>
      ),
    },
    data_node_status: {
      renderCell: (_status: DataNodeStatus, dataNode: DataNode) => <DataNodeStatusCell dataNode={dataNode} />,
    },
    is_leader: {
      renderCell: (_is_leader: string, dataNode: DataNode) => (dataNode.is_leader ? 'yes' : 'no'),
    },
    cert_valid_until: {
      renderCell: (_cert_valid_until: string, dataNode: DataNode) => <RelativeTime dateTime={dataNode.cert_valid_until} />,
    },
  },
};

const entityActions = (dataNode: DataNode) => (
  <DataNodeActions dataNode={dataNode} />
);

const DataNodeList = () => (
  <PaginatedEntityTable<DataNode> humanName="data nodes"
                                  columnsOrder={COLUMNS_ORDER}
                                  queryHelpComponent={(
                                    <QueryHelper entityName="datanode"
                                                 commonFields={['name']}
                                                 example={(
                                                   <p>
                                                     Find entities with a description containing node:<br />
                                                     <code>name:node</code><br />
                                                   </p>
                                            )} />
                             )}
                                  bulkSelection={{ actions: <DataNodeBulkActions /> }}
                                  entityActions={entityActions}
                                  tableLayout={DEFAULT_LAYOUT}
                                  fetchEntities={fetchDataNodes}
                                  fetchOptions={{ refetchInterval: 5000 }}
                                  keyFn={keyFn}
                                  entityAttributesAreCamelCase={false}
                                  columnRenderers={columnRenderers} />

);

export default DataNodeList;
