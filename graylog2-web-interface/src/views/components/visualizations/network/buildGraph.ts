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
import type { Key } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { LeafPath } from 'views/components/visualizations/utils/extractLeafPaths';

export type NetworkNode = {
  label: string;
  field: string;
  value: Key;
  degree: number;
};

export type NetworkEdge = {
  source: number;
  target: number;
  value: number;
};

export type Graph = {
  nodes: Array<NetworkNode>;
  edges: Array<NetworkEdge>;
};

/**
 * Build a node-and-edge graph from per-row pivot paths.
 *
 * Nodes are unified by their string label across stages — a value appearing in
 * multiple grouping fields produces one node. The first occurrence determines
 * the node's `field` and `value` metadata (used by the click popover).
 *
 * Edges chain consecutive stages: `keys[i] → keys[i+1]` for each path. Duplicate
 * (source, target) pairs are summed.
 */
const buildGraph = (paths: ReadonlyArray<LeafPath>, allFields: ReadonlyArray<string>): Graph => {
  const nodeIndex = new Map<string, number>();
  const nodes: Array<NetworkNode> = [];

  const indexOf = (label: string, stage: number, originalValue: Key): number => {
    const existing = nodeIndex.get(label);

    if (existing !== undefined) return existing;

    const idx = nodes.length;
    nodeIndex.set(label, idx);
    nodes.push({ label, field: allFields[stage], value: originalValue, degree: 0 });

    return idx;
  };

  const edgeAgg = new Map<string, NetworkEdge>();

  paths.forEach((path) => {
    for (let i = 0; i < path.keys.length - 1; i += 1) {
      const sourceLabel = String(path.keys[i]);
      const targetLabel = String(path.keys[i + 1]);
      const source = indexOf(sourceLabel, i, path.keys[i]);
      const target = indexOf(targetLabel, i + 1, path.keys[i + 1]);
      const edgeKey = `${source}\u0000${target}`;
      const existing = edgeAgg.get(edgeKey);

      if (existing) {
        existing.value += path.value;
      } else {
        edgeAgg.set(edgeKey, { source, target, value: path.value });
        nodes[source].degree += 1;
        nodes[target].degree += 1;
      }
    }
  });

  return { nodes, edges: Array.from(edgeAgg.values()) };
};

export default buildGraph;
