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
import React, { useCallback, useMemo, useState } from 'react';

import { EntityDataTable, Spinner } from 'components/common';
import type { Column } from 'components/common/EntityDataTable';

import useGraylogNodes from './useGraylogNodes';
import type { GraylogNode } from './useGraylogNodes';
import GraylogNodeActions from './GraylogNodeActions';
import ClusterNodesSectionWrapper from './ClusterNodesSectionWrapper';
import {
  DEFAULT_VISIBLE_COLUMNS,
  createColumnDefinitions,
  createColumnRenderers,
} from './GraylogNodesColumnConfiguration';

type Props = {
  collapsible?: boolean;
};

const GraylogNodesExpandable = ({ collapsible = true }: Props) => {
  const { nodes: graylogNodes, isLoading } = useGraylogNodes();
  const columnsOrder = useMemo<Array<string>>(() => [...DEFAULT_VISIBLE_COLUMNS], []);
  const [visibleColumns, setVisibleColumns] = useState<Array<string>>([...DEFAULT_VISIBLE_COLUMNS]);

  const columnDefinitions = useMemo<Array<Column>>(() => createColumnDefinitions(), []);
  const columnRenderers = useMemo(() => createColumnRenderers(), []);

  const handleColumnsChange = useCallback((newColumns: Array<string>) => {
    if (!newColumns.length) {
      return;
    }

    setVisibleColumns(newColumns);
  }, []);

  const handleSortChange = useCallback(() => {}, []);
  const renderActions = useCallback((entity: GraylogNode) => <GraylogNodeActions node={entity} />, []);

  return (
    <ClusterNodesSectionWrapper
      title="Graylog Nodes"
      headerLeftSection={isLoading && <Spinner />}
      collapsible={collapsible}>
      <EntityDataTable<GraylogNode>
        entities={graylogNodes}
        visibleColumns={visibleColumns}
        columnsOrder={columnsOrder}
        onColumnsChange={handleColumnsChange}
        onSortChange={handleSortChange}
        entityAttributesAreCamelCase
        entityActions={renderActions}
        columnDefinitions={columnDefinitions}
        columnRenderers={columnRenderers}
        actionsCellWidth={160}
      />
    </ClusterNodesSectionWrapper>
  );
};

export default GraylogNodesExpandable;
