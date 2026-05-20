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
import type { HealthNode } from './HealthReport.types';
import { isHealthFeature } from './HealthReport.types';

export const formatLeafCount = (node: HealthNode): string | undefined => {
  if (!node.total_affected) return undefined;
  if (typeof node.total === 'number') return `${node.total_affected}/${node.total}`;

  return `${node.total_affected}`;
};

export const formatLeafCountVerbose = (node: HealthNode, noun?: string): string | undefined => {
  if (!node.total_affected) return undefined;
  const nounPart = noun ? ` ${noun}` : '';
  if (typeof node.total === 'number') return `${node.total_affected} of ${node.total}${nounPart} affected`;

  return `${node.total_affected}${nounPart} affected`;
};

export const countContainedChecks = (node: HealthNode): number => {
  if (!isHealthFeature(node)) return 1;

  return node.children.reduce((count, child) => count + countContainedChecks(child), 0);
};

export const collectUnhealthyExpansionIds = (node: HealthNode): string[] => {
  if (!isHealthFeature(node) || node.status === 'healthy') return [];

  return [node.id, ...node.children.flatMap(collectUnhealthyExpansionIds)];
};

export const collectDescendantFeatureIds = (node: HealthNode): string[] => {
  if (!isHealthFeature(node)) return [];

  return node.children.flatMap((child) =>
    isHealthFeature(child) ? [child.id, ...collectDescendantFeatureIds(child)] : [],
  );
};
