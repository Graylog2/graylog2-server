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
import { useState } from 'react';
import styled, { css } from 'styled-components';

import Alert from 'components/bootstrap/Alert';
import Button from 'components/bootstrap/Button';
import SegmentedControl from 'components/bootstrap/SegmentedControl';

import { layout } from './mapLayout';
import type { Container, GroupBy } from './mapLayout';
import IndexPile from './IndexPile';

import { plural } from '../format';
import type { ShardMapResponse } from '../../types';

const Toolbar = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: ${theme.spacings.md};
    margin-bottom: ${theme.spacings.md};
  `,
);

const Muted = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
  `,
);

const Containers = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    gap: ${theme.spacings.md};
  `,
);

// Nodes: rounded squares. Indices inside: smaller rounded boxes holding a woodpile of shard logs (IndexPile).
// Lines get thinner from the outside in, and each level has its own background (node: the page's, index: the
// panes', shard: its colour), so neighbours don't run together.
const ContainerBox = styled.section<{ $kind: Container['kind'] }>(
  ({ theme, $kind }) => css`
    // Dashed: copies on no node, or on a node that left; the same grey as the nodes, the shards carry the colour.
    border: ${$kind === 'unplaced' || $kind === 'left' ? '3px dashed' : '3px solid'} ${theme.colors.gray[60]};
    border-radius: ${$kind === 'index' ? '0' : '14px'};
    background-color: ${theme.colors.global.background};
    padding: ${theme.spacings.sm};
    min-width: 160px;
    max-width: 100%;
  `,
);

const ContainerTitle = styled.h4(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.body};
    margin: 0 0 ${theme.spacings.xs};
    display: flex;
    gap: ${theme.spacings.sm};
    align-items: baseline;
  `,
);

// Each index keeps its own height: opening one doesn't stretch its neighbours.
const Indices = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    gap: ${theme.spacings.sm};
  `,
);

// The colour legend takes the room left and wraps inside it, so the scope stays on the same line.
const Legend = styled.span(
  ({ theme }) => css`
    flex: 1 1 320px;
    color: ${theme.colors.text.secondary};
  `,
);

// How much of the cluster is drawn, and the switch to draw all of it: at the end of the toolbar, as quiet as the
// colour legend, next to the other controls that change what the map shows.
const Scope = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: ${theme.spacings.sm};
    margin-left: auto;
    color: ${theme.colors.text.secondary};
  `,
);

const GROUP_BY: Array<{ value: GroupBy; label: string }> = [
  { value: 'node', label: 'By node' },
  { value: 'index', label: 'By index' },
  { value: 'cause', label: 'By cause' },
];

type Props = {
  map: ShardMapResponse;
  shardCounts: { [index: string]: number };
  picked: Set<string>;
  onTogglePicked: (index: string) => void;
};

// The user's map: nodes holding indices holding shard copies; only copies with a problem, plus whole indices on
// request. Scales to many nodes because nodes without problem copies are only counted.
const AllocationMap = ({ map, shardCounts, picked, onTogglePicked }: Props) => {
  const [groupBy, setGroupBy] = useState<GroupBy>('node');
  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const [selected, setSelected] = useState<string | undefined>(undefined);
  const [showAll, setShowAll] = useState(false);
  const { containers, quietNodes } = layout(map, groupBy, expanded, showAll);
  const healthyCopies = map.shards.filter((copy) => copy.state === 'STARTED').length;

  const scope = (() => {
    if (showAll) {
      return `Showing every node and all ${healthyCopies} healthy shard copies.`;
    }

    if (quietNodes > 0) {
      return `${plural(quietNodes, 'other node holds', 'other nodes hold')} only healthy shards; healthy copies are hidden.`;
    }

    return `${plural(healthyCopies, 'healthy shard copy is', 'healthy shard copies are')} hidden.`;
  })();

  const toggleExpanded = (index: string) =>
    setExpanded((current) => {
      const next = new Set(current);

      if (next.has(index)) {
        next.delete(index);
      } else {
        next.add(index);
      }

      return next;
    });

  return (
    <>
      <Toolbar>
        <SegmentedControl<GroupBy> data={GROUP_BY} value={groupBy} onChange={setGroupBy} />
        <Legend>
          Red: primary not placed (index red). Yellow: replica not placed. Blue: starting or moving. Green: healthy.
          Click a shard for what happened and what to do.
        </Legend>
        <Scope>
          <span>{scope}</span>
          <Button bsSize="xsmall" onClick={() => setShowAll(!showAll)} aria-pressed={showAll}>
            {showAll ? 'Show problems only' : `Show ${quietNodes > 0 ? 'all nodes' : 'everything'}`}
          </Button>
        </Scope>
      </Toolbar>
      {containers.length === 0 && <Alert bsStyle="success">Every shard is placed and healthy.</Alert>}
      <Containers>
        {containers.map((container) => (
          <ContainerBox key={container.key} $kind={container.kind} aria-label={container.title}>
            <ContainerTitle>
              <span>{container.title}</span>
              {container.problems > 0 && <Muted>{plural(container.problems, 'problem', 'problems')}</Muted>}
            </ContainerTitle>
            <Indices>
              {container.indices.length === 0 && <Muted>No shards</Muted>}
              {container.indices.map((group) => (
                <IndexPile
                  key={group.index}
                  group={group}
                  expanded={expanded.has(group.index)}
                  onToggleExpanded={() => toggleExpanded(group.index)}
                  selected={selected}
                  onSelect={setSelected}
                  shardCounts={shardCounts}
                  picked={picked}
                  onTogglePicked={onTogglePicked}
                />
              ))}
            </Indices>
          </ContainerBox>
        ))}
      </Containers>
    </>
  );
};

export default AllocationMap;
