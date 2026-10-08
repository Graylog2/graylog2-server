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
// Arranges the shard map into containers: nodes (or indices, or causes) holding indices holding shard copies.
// Only copies with a problem are shown, plus every copy of the indices the user opened, so the map stays small
// on large clusters: healthy nodes are only counted.
import { situationTitle } from '../diagnosisText';
import type { MapCopy, ShardMapResponse } from '../../types';

export type GroupBy = 'node' | 'index' | 'cause';

// `hiddenElsewhere`: copies of this index not on the map at all (healthy ones, until the index is opened).
export type IndexGroup = { index: string; copies: Array<MapCopy>; hiddenElsewhere: number };

export type ContainerKind = 'node' | 'unplaced' | 'left' | 'index' | 'cause';

export type Container = {
  key: string;
  kind: ContainerKind;
  title: string;
  indices: Array<IndexGroup>;
  problems: number;
};

export type MapLayout = {
  containers: Array<Container>;
  // Nodes without a copy on the map: shown as a count, not as boxes.
  quietNodes: number;
};

const UNPLACED = '\u0000unplaced';
const LEFT = '\u0000left';

export const hasProblem = (copy: MapCopy) => copy.state !== 'STARTED';

const compareText = (a: string, b: string) => a.localeCompare(b, undefined, { numeric: true, sensitivity: 'base' });

// Where a copy goes when grouped by node: the node it is on; for an unassigned copy the node its recovery failed on
// (its data is there), else a container for copies whose node left, else one for copies on no node at all.
const nodeKey = (copy: MapCopy) => copy.node ?? copy.failed_on_node ?? (copy.left_node ? LEFT : UNPLACED);

const causeKey = (copy: MapCopy) => {
  if (copy.state === 'UNASSIGNED') {
    return copy.situation ?? 'UNKNOWN';
  }

  return copy.state;
};

const STATE_TITLES = { STARTED: 'Healthy', INITIALIZING: 'Starting', RELOCATING: 'Moving to another node' };

const containerFor = (key: string, groupBy: GroupBy): Pick<Container, 'kind' | 'title'> => {
  if (groupBy === 'index') {
    return { kind: 'index', title: key };
  }

  if (groupBy === 'cause') {
    return { kind: 'cause', title: STATE_TITLES[key] ?? situationTitle(key as MapCopy['situation']) ?? key };
  }

  if (key === UNPLACED) {
    return { kind: 'unplaced', title: 'Not on any node' };
  }

  if (key === LEFT) {
    return { kind: 'left', title: 'On a node that left the cluster' };
  }

  return { kind: 'node', title: key };
};

const KIND_ORDER: { [kind in ContainerKind]: number } = { unplaced: 0, left: 1, node: 2, index: 2, cause: 2 };

const byPrimaryFirst = (a: MapCopy, b: MapCopy) => a.shard - b.shard || Number(b.primary) - Number(a.primary);

// `showAll`: every copy on the map, healthy ones included, and (by node) also nodes that hold nothing.
export const layout = (map: ShardMapResponse, groupBy: GroupBy, expanded: Set<string>, showAll = false): MapLayout => {
  const visible = map.shards.filter((copy) => showAll || hasProblem(copy) || expanded.has(copy.index));
  const keyOf = { node: nodeKey, index: (copy: MapCopy) => copy.index, cause: causeKey }[groupBy];

  const containers = new Map<string, Map<string, Array<MapCopy>>>();

  if (showAll && groupBy === 'node') {
    map.nodes.forEach((node) => containers.set(node.name, new Map()));
  }

  visible.forEach((copy) => {
    const key = keyOf(copy);
    const indices = containers.get(key) ?? new Map<string, Array<MapCopy>>();
    indices.set(copy.index, [...(indices.get(copy.index) ?? []), copy]);
    containers.set(key, indices);
  });

  const totalPerIndex = new Map<string, number>();
  map.shards.forEach((copy) => totalPerIndex.set(copy.index, (totalPerIndex.get(copy.index) ?? 0) + 1));
  const shownPerIndex = new Map<string, number>();
  visible.forEach((copy) => shownPerIndex.set(copy.index, (shownPerIndex.get(copy.index) ?? 0) + 1));

  const result: Array<Container> = [...containers.entries()].map(([key, indices]) => {
    const groups = [...indices.entries()]
      .map(([index, copies]) => ({
        index,
        copies: [...copies].sort(byPrimaryFirst),
        hiddenElsewhere: (totalPerIndex.get(index) ?? 0) - (shownPerIndex.get(index) ?? 0),
      }))
      .sort((a, b) => compareText(a.index, b.index));

    return {
      key,
      ...containerFor(key, groupBy),
      indices: groups,
      problems: groups.reduce((sum, group) => sum + group.copies.filter(hasProblem).length, 0),
    };
  });

  // Most urgent first: copies on no node, then the boxes with the most problems.
  result.sort(
    (a, b) => KIND_ORDER[a.kind] - KIND_ORDER[b.kind] || b.problems - a.problems || compareText(a.title, b.title),
  );

  const shownNodes = groupBy === 'node' ? result.filter((container) => container.kind === 'node').length : 0;

  return { containers: result, quietNodes: groupBy === 'node' ? map.nodes.length - shownNodes : 0 };
};

// A closed index shows this many shards, problems first; opening it shows the rest.
export const COLLAPSED_SHARDS = 3;

/** Which copies an index shows, and what its toggle offers: more here, the healthy ones elsewhere, or nothing. */
export const indexContent = (group: IndexGroup, expanded: boolean) => {
  // Problems first, so a closed index shows them; within that, the shard order from layout() (stable sort).
  const ordered = [...group.copies].sort((a, b) => Number(hasProblem(b)) - Number(hasProblem(a)));
  const shown = expanded ? ordered : ordered.slice(0, COLLAPSED_SHARDS);
  const hiddenHere = ordered.length - shown.length;

  const toggle = (() => {
    if (expanded) {
      return 'fewer';
    }

    if (hiddenHere > 0) {
      return `+${hiddenHere} more`;
    }

    return group.hiddenElsewhere > 0 ? 'all shards' : null;
  })();

  return { shown, toggle };
};

/**
 * Row sizes of a woodpile of `count` logs, top row first: the bottom row is the narrowest that can carry the rest in
 * rows one shorter each (3 → [1, 2], 6 → [1, 2, 3], 7 → [3, 4]).
 */
export const pileRows = (count: number): Array<number> => {
  if (count <= 0) {
    return [];
  }

  let base = 1;

  while ((base * (base + 1)) / 2 < count) {
    base += 1;
  }

  const rows: Array<number> = [];
  let left = count;

  for (let width = base; left > 0; width -= 1) {
    const row = Math.min(width, left);
    rows.unshift(row);
    left -= row;
  }

  return rows;
};

/** For a screen reader and the hover title: which copy, and what is wrong with it. */
export const copyLabel = (copy: MapCopy) => {
  const what = `${copy.index}, shard ${copy.shard}, ${copy.primary ? 'primary' : 'replica'}`;

  switch (copy.state) {
    case 'UNASSIGNED':
      return `${what}: unassigned, ${situationTitle(copy.situation ?? 'UNKNOWN').toLowerCase()}`;
    case 'STARTED':
      return `${what}: healthy`;
    default:
      return `${what}: ${(STATE_TITLES[copy.state] ?? copy.state).toLowerCase()}`;
  }
};
