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
import type { useTree } from '@mantine/core';

import { Icon } from 'components/common';

import HealthStatusIcon from './HealthStatusIcon';
import { collectDescendantFeatureIds, collectUnhealthyExpansionIds } from './healthTree';
import { STATUS_LABELS } from './healthStatusCopy';
import type { HealthFeature, HealthNode, HealthStatus } from './HealthReport.types';
import type { HealthTreeDataNode } from './useHealthModule';
import { ChevronSlot, StyledTree, TreeCountSuffix, TreeLabel, TreePane, TreeRow } from './HealthModule.styles';

type Props = {
  tree: ReturnType<typeof useTree>;
  treeData: HealthTreeDataNode[];
  lookup: Record<string, HealthNode>;
  root: HealthFeature;
};

const HealthTreePane = ({ tree, treeData, lookup, root }: Props) => {
  const handleRowClick = (
    event: React.MouseEvent,
    nodeValue: string,
    nodeStatus: HealthStatus,
    isFeature: boolean,
    defaultOnClick?: (e: React.MouseEvent) => void,
  ) => {
    if (nodeValue === root.id) {
      tree.select(root.id);
      tree.expand(root.id);

      return;
    }

    // Snapshot expansion BEFORE letting Mantine toggle — defaultOnClick mutates the state.
    const wasExpanded = tree.expandedState[nodeValue];
    defaultOnClick?.(event);

    if (!isFeature) return;

    const subject = lookup[nodeValue];

    if (!subject) return;

    if (wasExpanded) {
      collectDescendantFeatureIds(subject).forEach((id) => tree.collapse(id));
    } else if (nodeStatus !== 'healthy') {
      collectUnhealthyExpansionIds(subject).forEach((id) => tree.expand(id));
    }
  };

  return (
    <TreePane>
      <StyledTree
        aria-label="Cluster health tree"
        data={treeData}
        tree={tree}
        expandOnClick
        selectOnClick
        renderNode={({ expanded, hasChildren, node, selected, elementProps }) => {
          const nodeProps = (node as HealthTreeDataNode).nodeProps;
          const isRootNode = node.value === root.id;

          return (
            <TreeRow
              {...elementProps}
              $selected={selected}
              aria-label={`${node.label}, ${STATUS_LABELS[nodeProps.status]}`}
              onClick={(event) =>
                handleRowClick(event, node.value, nodeProps.status, nodeProps.isFeature, elementProps.onClick)
              }>
              <ChevronSlot>
                {hasChildren && !isRootNode ? (
                  <Icon name={expanded ? 'keyboard_arrow_down' : 'keyboard_arrow_right'} size="sm" />
                ) : null}
              </ChevronSlot>
              <HealthStatusIcon status={nodeProps.status} title={STATUS_LABELS[nodeProps.status]} />
              <TreeLabel $emphasized={hasChildren}>{node.label}</TreeLabel>
              {nodeProps.countSummary ? <TreeCountSuffix>({nodeProps.countSummary})</TreeCountSuffix> : null}
            </TreeRow>
          );
        }}
      />
    </TreePane>
  );
};

export default HealthTreePane;
