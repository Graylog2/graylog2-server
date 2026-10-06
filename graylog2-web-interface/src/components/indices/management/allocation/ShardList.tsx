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

import Button from 'components/bootstrap/Button';
import Label from 'components/bootstrap/Label';
import Table from 'components/bootstrap/Table';

import { byIndex, formatTime, plural } from './format';
import IndexShards, { DetailsCell, Muted, Row, Toggle } from './IndexShards';

import type { ShardExplanation } from '../types';

// Picked indices (in the list below) are bold with a tick; picking one again takes it out.
const IndexLink = styled(Button)<{ $picked: boolean }>(
  ({ $picked }) => css`
    padding: 0;
    font-family: monospace;
    font-weight: ${$picked ? 'bold' : 'normal'};
  `,
);

// The shards of an opened index, set in from the left.
const ShardsCell = styled(DetailsCell)(
  ({ theme }) => css`
    padding-left: ${theme.spacings.xl} !important;
  `,
);

const COLUMNS = 6;

type Props = {
  shards: Array<ShardExplanation>;
  picked: Set<string>;
  onTogglePicked: (index: string) => void;
  // Primary shard count per index, for "shard 0 of 3".
  shardCounts: { [index: string]: number };
};

// One line per index with unassigned shards: how many, why, and where the data is. Opening a line lists its shards,
// each of which opens what happened in full and what can be done. The index name picks it for the list below.
const ShardList = ({ shards, picked, onTogglePicked, shardCounts }: Props) => {
  const [open, setOpen] = useState<Set<string>>(new Set());

  const toggle = (index: string) =>
    setOpen((current) => {
      const next = new Set(current);

      if (next.has(index)) {
        next.delete(index);
      } else {
        next.add(index);
      }

      return next;
    });

  return (
    <Table condensed hover>
      <thead>
        <tr>
          <th aria-label="Open" />
          <th>Index</th>
          <th>Unassigned shards</th>
          <th>What happened</th>
          <th>Data on</th>
          <th>Unassigned since</th>
        </tr>
      </thead>
      <tbody>
        {byIndex(shards).map((group) => {
          const isOpen = open.has(group.index);
          const isPicked = picked.has(group.index);

          return (
            <React.Fragment key={group.index}>
              <Row $open={isOpen} onClick={() => toggle(group.index)}>
                <td>
                  <Toggle
                    type="button"
                    aria-expanded={isOpen}
                    aria-label={`${isOpen ? 'Close' : 'Open'} ${group.index}`}
                    onClick={(event) => {
                      event.stopPropagation();
                      toggle(group.index);
                    }}>
                    {isOpen ? '▾' : '▸'}
                  </Toggle>
                </td>
                <td>
                  <IndexLink
                    bsStyle="link"
                    $picked={isPicked}
                    aria-pressed={isPicked}
                    title={isPicked ? 'Remove this index from the list below' : 'Add this index to the list below'}
                    onClick={(event) => {
                      event.stopPropagation();
                      onTogglePicked(group.index);
                    }}>
                    {isPicked && '✓ '}
                    {group.index}
                  </IndexLink>
                </td>
                <td>
                  {group.primaries > 0 && (
                    <Label bsStyle="danger">{plural(group.primaries, 'primary', 'primaries')}</Label>
                  )}{' '}
                  {group.replicas > 0 && (
                    <Label bsStyle="warning">{plural(group.replicas, 'replica', 'replicas')}</Label>
                  )}
                </td>
                <td>
                  {group.situations.length > 1 && <Muted>{group.situations.length} causes: </Muted>}
                  {group.situations.join('; ')}
                </td>
                <td>{group.dataOn.join(', ')}</td>
                <td>{formatTime(group.since)}</td>
              </Row>
              {isOpen && (
                <tr>
                  <ShardsCell colSpan={COLUMNS}>
                    <IndexShards shards={group.shards} totalShards={shardCounts[group.index]} />
                  </ShardsCell>
                </tr>
              )}
            </React.Fragment>
          );
        })}
      </tbody>
    </Table>
  );
};

export default ShardList;
