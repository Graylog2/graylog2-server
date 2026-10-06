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
import styled, { css } from 'styled-components';
import type { ColorVariant } from '@graylog/sawmill';

import Button from 'components/bootstrap/Button';
import Label from 'components/bootstrap/Label';
import Table from 'components/bootstrap/Table';

import DiagnosisSummary from './DiagnosisSummary';
import { dataOn, formatTime, plural } from './format';

import type { ShardExplanation } from '../types';

const CAN_ALLOCATE_STYLES: { [canAllocate: string]: ColorVariant } = {
  no: 'danger',
  throttled: 'warning',
  awaiting_info: 'warning',
  allocation_delayed: 'warning',
  yes: 'success',
};

const Group = styled.div(
  ({ theme }) => css`
    border-top: 1px solid ${theme.colors.gray[90]};
    padding-top: 10px;
    margin-top: 10px;
  `,
);

const Muted = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
  `,
);

const IndexLink = styled(Button)`
  padding: 0;
  font-family: monospace;
`;

// What OpenSearch said, word for word, collapsed below the plain-language summary.
const Reported = styled.details`
  margin-bottom: 6px;

  summary {
    cursor: pointer;
  }

  pre {
    white-space: pre-wrap;
    max-height: 200px;
    overflow-y: auto;
    font-size: 0.85em;
  }
`;

const ReportedList = styled.ul`
  margin-bottom: 6px;
  word-break: break-word;
`;

type ShardsProps = {
  shards: Array<ShardExplanation>;
  onShowIndex: (index: string) => void;
};

const ShardTable = ({ shards, onShowIndex }: ShardsProps) => (
  <Table condensed>
    <thead>
      <tr>
        <th>Index</th>
        <th>Shard</th>
        <th>Copy</th>
        <th>Data on</th>
        <th>Unassigned because</th>
        <th>Since</th>
        <th>Failed attempts</th>
      </tr>
    </thead>
    <tbody>
      {shards.map((shard) => (
        <tr key={`${shard.index}-${shard.shard}-${shard.primary}`}>
          <td>
            <IndexLink bsStyle="link" title="Show this index in the list below" onClick={() => onShowIndex(shard.index)}>
              {shard.index}
            </IndexLink>
          </td>
          <td>{shard.shard}</td>
          <td>{shard.primary ? <Label bsStyle="danger">primary</Label> : <Label bsStyle="warning">replica</Label>}</td>
          <td>{dataOn(shard)}</td>
          <td>{shard.unassigned_reason}</td>
          <td>{formatTime(shard.unassigned_since)}</td>
          <td>{shard.failed_attempts ?? ''}</td>
        </tr>
      ))}
    </tbody>
  </Table>
);

const WhatOpenSearchReported = ({ shards }: { shards: Array<ShardExplanation> }) => {
  const [first] = shards;
  const nodes = first.nodes.filter((node) => node.deciders.length > 0);

  return (
    <Reported>
      <summary>What OpenSearch reported</summary>
      <p>
        <Label bsStyle={CAN_ALLOCATE_STYLES[first.can_allocate] ?? 'default'}>
          can allocate: {first.can_allocate ?? 'unknown'}
        </Label>{' '}
        {first.explanation}
      </p>
      <ReportedList>
        {shards
          .filter((shard) => shard.root_cause || shard.details)
          .map((shard) => (
            <li key={`${shard.index}-${shard.shard}-${shard.primary}`}>
              <code>
                {shard.index}[{shard.shard}]
              </code>{' '}
              {shard.root_cause}
              {shard.details && (
                <details>
                  <summary>Full details</summary>
                  <pre>{shard.details}</pre>
                </details>
              )}
            </li>
          ))}
      </ReportedList>
      {nodes.length > 0 && (
        <>
          <Muted>Per node, only the checks that said no:</Muted>
          <ReportedList>
            {nodes.map((node) => (
              <li key={node.node_name}>
                <strong>{node.node_name}</strong>
                <ul>
                  {node.deciders.map((decider) => (
                    <li key={decider.decider}>
                      <code>{decider.decider}</code> ({decider.decision}): {decider.explanation}
                    </li>
                  ))}
                </ul>
              </li>
            ))}
          </ReportedList>
        </>
      )}
    </Reported>
  );
};

type Props = ShardsProps & {
  // Primary shard count per index, for "shard 0 of 3".
  shardCounts: { [index: string]: number };
};

// One situation: what happened in plain words, the shards it applies to, then OpenSearch's own words collapsed.
const ExplanationGroup = ({ shards, onShowIndex, shardCounts }: Props) => {
  const [first] = shards;

  return (
    <Group>
      {first.error && (
        <p>
          <Label bsStyle="default">not explained</Label> OpenSearch returned an error for{' '}
          {plural(shards.length, 'shard', 'shards')}: {first.error}
        </p>
      )}
      {first.diagnosis && (
        <DiagnosisSummary
          diagnosis={first.diagnosis}
          context={{
            index: first.index,
            shard: first.shard,
            primary: first.primary,
            totalShards: shardCounts[first.index],
          }}
        />
      )}
      {shards.length > 1 && <Muted>The same applies to all {shards.length} shards below.</Muted>}
      <ShardTable shards={shards} onShowIndex={onShowIndex} />
      {!first.error && <WhatOpenSearchReported shards={shards} />}
    </Group>
  );
};

export default ExplanationGroup;
