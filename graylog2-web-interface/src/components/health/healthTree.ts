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
import type { HealthCheck, HealthNode } from './HealthReport.types';
import { isHealthFeature } from './HealthReport.types';

export const formatLeafCount = (check: HealthCheck): string | undefined => {
  if (check.total_affected === 0) return undefined;
  if (typeof check.total === 'number') return `${check.total_affected}/${check.total}`;

  return `${check.total_affected}`;
};

export const countContainedChecks = (node: HealthNode): number => {
  if (!isHealthFeature(node)) return 1;

  return node.children.reduce((count, child) => count + countContainedChecks(child), 0);
};

/**
 * Returns the ids of every non-healthy feature in the subtree rooted at `node`,
 * inclusive of `node` itself when it is a non-healthy feature. Used to expand
 * the path to non-healthy leaves when an unhealthy feature is opened.
 */
export const collectUnhealthyExpansionIds = (node: HealthNode): string[] => {
  if (!isHealthFeature(node) || node.status === 'healthy') return [];

  return [node.id, ...node.children.flatMap(collectUnhealthyExpansionIds)];
};

/**
 * Returns the ids of every descendant feature of `node`, exclusive of `node`
 * itself. Used to cascade-collapse a subtree so reopening starts fresh.
 */
export const collectDescendantFeatureIds = (node: HealthNode): string[] => {
  if (!isHealthFeature(node)) return [];

  return node.children.flatMap((child) =>
    isHealthFeature(child) ? [child.id, ...collectDescendantFeatureIds(child)] : [],
  );
};
