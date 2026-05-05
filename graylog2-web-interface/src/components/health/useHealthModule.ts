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
import { useMemo } from 'react';
import type { TreeNodeData } from '@mantine/core';

import type { HealthFeature, HealthNode, HealthReport, HealthStatus } from './HealthReport.types';
import { isHealthFeature } from './HealthReport.types';
import { formatLeafCount } from './healthTree';
import mockHealthReport from './mockHealthReport';

export const SYNTHETIC_ROOT_ID = 'cluster_health';
export const SYNTHETIC_ROOT_TITLE = 'Cluster Health';

type TreeExpandedState = Record<string, boolean>;

export type HealthTreeDataNode = TreeNodeData & {
  nodeProps: { isFeature: boolean; status: HealthStatus; countSummary?: string };
  children?: HealthTreeDataNode[];
};

export type HealthModuleState = {
  report: HealthReport;
  root: HealthFeature;
  treeData: HealthTreeDataNode[];
  lookup: Record<string, HealthNode>;
  paths: Record<string, string[]>;
  initialExpandedState: TreeExpandedState;
};

const toTreeData = (node: HealthNode): HealthTreeDataNode => ({
  value: node.id,
  label: node.title,
  nodeProps: {
    isFeature: isHealthFeature(node),
    status: node.status,
    countSummary: isHealthFeature(node) ? undefined : formatLeafCount(node),
  },
  children: isHealthFeature(node) ? node.children.map(toTreeData) : undefined,
});

const buildLookup = (node: HealthNode): Record<string, HealthNode> => {
  const childLookup = isHealthFeature(node) ? Object.assign({}, ...node.children.map(buildLookup)) : {};

  return { [node.id]: node, ...childLookup };
};

const buildPaths = (node: HealthNode, parentPath: string[] = []): Record<string, string[]> => {
  const path = [...parentPath, node.title];
  const childPaths = isHealthFeature(node)
    ? Object.assign({}, ...node.children.map((child) => buildPaths(child, path)))
    : {};

  return { [node.id]: path, ...childPaths };
};

const buildHealthModuleState = (report: HealthReport): HealthModuleState => {
  const root: HealthFeature = {
    id: SYNTHETIC_ROOT_ID,
    title: SYNTHETIC_ROOT_TITLE,
    status: report.overall_status,
    children: report.features,
  };

  return {
    report,
    root,
    treeData: [toTreeData(root)],
    lookup: buildLookup(root),
    paths: buildPaths(root),
    initialExpandedState: { [root.id]: true },
  };
};

const useHealthModule = (): HealthModuleState =>
  useMemo(() => buildHealthModuleState(mockHealthReport), []);

export type HealthSummary = {
  overallStatus: HealthStatus;
  nonHealthyCount: number;
};

const countNonHealthyLeaves = (node: HealthNode): number => {
  if (!isHealthFeature(node)) return node.status === 'healthy' ? 0 : 1;

  return node.children.reduce((count, child) => count + countNonHealthyLeaves(child), 0);
};

export const useHealthSummary = (): HealthSummary =>
  useMemo(
    () => ({
      overallStatus: mockHealthReport.overall_status,
      nonHealthyCount: mockHealthReport.features.reduce(
        (count, feature) => count + countNonHealthyLeaves(feature),
        0,
      ),
    }),
    [],
  );

export default useHealthModule;
