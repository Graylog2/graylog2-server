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
import buildGraph from '../buildGraph';

describe('buildGraph', () => {
  it('builds unique nodes and edges from 2-stage paths', () => {
    const { nodes, edges } = buildGraph(
      [
        { keys: ['a1', 'b1'], value: 5 },
        { keys: ['a1', 'b2'], value: 3 },
        { keys: ['a2', 'b1'], value: 7 },
      ],
      ['source', 'target'],
    );

    expect(nodes.map((n) => n.label)).toEqual(['a1', 'b1', 'b2', 'a2']);
    expect(edges).toEqual([
      { source: 0, target: 1, value: 5 },
      { source: 0, target: 2, value: 3 },
      { source: 3, target: 1, value: 7 },
    ]);
  });

  it('unifies same value appearing in different stages into a single node', () => {
    const { nodes, edges } = buildGraph(
      [
        { keys: ['x', 'y'], value: 1 },
        { keys: ['y', 'x'], value: 1 },
      ],
      ['source', 'target'],
    );

    expect(nodes.map((n) => n.label)).toEqual(['x', 'y']);
    expect(edges).toEqual([
      { source: 0, target: 1, value: 1 },
      { source: 1, target: 0, value: 1 },
    ]);
  });

  it('chains edges and sums shared (source,target) pairs across 3 stages', () => {
    const { nodes, edges } = buildGraph(
      [
        { keys: ['a1', 'b1', 'c1'], value: 2 },
        { keys: ['a1', 'b1', 'c2'], value: 3 },
        { keys: ['a1', 'b2', 'c1'], value: 4 },
        { keys: ['a2', 'b1', 'c2'], value: 6 },
      ],
      ['source', 'mid', 'target'],
    );

    expect(nodes.map((n) => n.label)).toEqual(['a1', 'b1', 'c1', 'c2', 'b2', 'a2']);

    const lookup = nodes.map((n) => n.label);
    const labelled = edges.map((e) => ({ from: lookup[e.source], to: lookup[e.target], value: e.value }));

    expect(labelled).toEqual([
      { from: 'a1', to: 'b1', value: 5 },
      { from: 'b1', to: 'c1', value: 2 },
      { from: 'b1', to: 'c2', value: 9 },
      { from: 'a1', to: 'b2', value: 4 },
      { from: 'b2', to: 'c1', value: 4 },
      { from: 'a2', to: 'b1', value: 6 },
    ]);
  });

  it('records per-node degree counting each distinct incident edge', () => {
    const { nodes } = buildGraph(
      [
        { keys: ['hub', 'a'], value: 1 },
        { keys: ['hub', 'b'], value: 1 },
        { keys: ['hub', 'c'], value: 1 },
      ],
      ['source', 'target'],
    );

    const byLabel = Object.fromEntries(nodes.map((n) => [n.label, n.degree]));

    expect(byLabel).toEqual({ hub: 3, a: 1, b: 1, c: 1 });
  });

  it('does not double-count degree when the same edge is aggregated', () => {
    const { nodes } = buildGraph(
      [
        { keys: ['a', 'b'], value: 1 },
        { keys: ['a', 'b'], value: 2 },
      ],
      ['source', 'target'],
    );

    expect(nodes).toEqual([
      { label: 'a', field: 'source', value: 'a', degree: 1 },
      { label: 'b', field: 'target', value: 'b', degree: 1 },
    ]);
  });

  it('records first-seen field/value for unified nodes', () => {
    const { nodes } = buildGraph(
      [
        { keys: ['x', 'y'], value: 1 },
        { keys: ['y', 'x'], value: 1 },
      ],
      ['source', 'target'],
    );

    expect(nodes.find((n) => n.label === 'x')).toEqual({ label: 'x', field: 'source', value: 'x', degree: 2 });
    expect(nodes.find((n) => n.label === 'y')).toEqual({ label: 'y', field: 'target', value: 'y', degree: 2 });
  });
});
