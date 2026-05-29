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
import type { TreeNodeData } from '@mantine/core';

import type { HealthFeature, HealthNode, HealthReport, HealthStatus } from './HealthReport.types';
import { isHealthFeature } from './HealthReport.types';
import { formatLeafCount } from './healthTree';
import mockHealthReport from './mockHealthReport';

const SYNTHETIC_ROOT_ID = 'cluster_health';
const SYNTHETIC_ROOT_TITLE = 'Cluster Health';

type TreeExpandedState = Record<string, boolean>;

export type HealthTreeDataNode = TreeNodeData & {
  nodeProps: { status: HealthStatus; countSummary?: string };
  children?: HealthTreeDataNode[];
};

type HealthModuleState = {
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
    status: node.status,
    countSummary: formatLeafCount(node),
  },
  children: isHealthFeature(node) ? node.children.map(toTreeData) : undefined,
});

const buildLookup = (root: HealthNode): Record<string, HealthNode> => {
  const lookup: Record<string, HealthNode> = {};

  const visit = (node: HealthNode) => {
    lookup[node.id] = node;

    if (isHealthFeature(node)) {
      node.children.forEach(visit);
    }
  };

  visit(root);

  return lookup;
};

const buildPaths = (root: HealthNode): Record<string, string[]> => {
  const paths: Record<string, string[]> = {};

  const visit = (node: HealthNode, parentPath: string[] = []) => {
    const path = [...parentPath, node.title];
    paths[node.id] = path;

    if (isHealthFeature(node)) {
      node.children.forEach((child) => visit(child, path));
    }
  };

  visit(root);

  return paths;
};

const buildHealthModuleState = (report: HealthReport): HealthModuleState => {
  const root: HealthFeature = {
    id: SYNTHETIC_ROOT_ID,
    title: SYNTHETIC_ROOT_TITLE,
    status: report.overall_status,
    children: report.features,
  };

  return {
    root,
    treeData: [toTreeData(root)],
    lookup: buildLookup(root),
    paths: buildPaths(root),
    initialExpandedState: { [root.id]: true },
  };
};

const HEALTH_MODULE_STATE = buildHealthModuleState(mockHealthReport);

const useHealthModule = (): HealthModuleState => HEALTH_MODULE_STATE;

type HealthSummary = {
  overallStatus: HealthStatus;
};

export const useHealthSummary = (): HealthSummary => ({
  overallStatus: mockHealthReport.overall_status,
});

export default useHealthModule;
