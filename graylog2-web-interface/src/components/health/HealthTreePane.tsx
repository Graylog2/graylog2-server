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
import { useEffect, useRef } from 'react';
import type { useTree } from '@mantine/core';

import { Icon } from 'components/common';

import HealthStatusIcon from './HealthStatusIcon';
import { collectDescendantFeatureIds, collectUnhealthyExpansionIds } from './healthTree';
import { STATUS_LABELS } from './healthStatusCopy';
import type { HealthFeature, HealthNode } from './HealthReport.types';
import { isHealthFeature } from './HealthReport.types';
import type { HealthTreeDataNode } from './useHealthModule';
import { ChevronSlot, StyledTree, TreeCountSuffix, TreeLabel, TreePane, TreeRow } from './HealthModule.styles';

type Props = {
  tree: ReturnType<typeof useTree>;
  treeData: HealthTreeDataNode[];
  lookup: Record<string, HealthNode>;
  root: HealthFeature;
};

const useExpansionCascade = (tree: ReturnType<typeof useTree>, lookup: Record<string, HealthNode>, rootId: string) => {
  const previousExpandedRef = useRef<Record<string, boolean>>(tree.expandedState);

  useEffect(() => {
    // Synthetic root must always stay expanded — re-assert if anything collapsed it.
    if (tree.expandedState[rootId] === false) {
      tree.expand(rootId);

      return;
    }

    const previous = previousExpandedRef.current;
    const current = tree.expandedState;

    const changedIds = Array.from(new Set([...Object.keys(previous), ...Object.keys(current)])).filter(
      (id) => Boolean(previous[id]) !== Boolean(current[id]),
    );

    previousExpandedRef.current = current;

    if (changedIds.length !== 1) return;

    const changedId = changedIds[0];
    const subject = lookup[changedId];

    if (!subject || !isHealthFeature(subject)) return;

    const isNowExpanded = Boolean(current[changedId]);

    if (!isNowExpanded) {
      collectDescendantFeatureIds(subject).forEach((id) => tree.collapse(id));
    } else if (subject.status !== 'healthy') {
      collectUnhealthyExpansionIds(subject).forEach((id) => tree.expand(id));
    }
  }, [tree, lookup, rootId]);
};

const HealthTreePane = ({ tree, treeData, lookup, root }: Props) => {
  useExpansionCascade(tree, lookup, root.id);

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
          const accessibleName = `${node.label}, ${STATUS_LABELS[nodeProps.status]}`;

          return (
            <TreeRow {...elementProps} $selected={selected} aria-label={accessibleName}>
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
