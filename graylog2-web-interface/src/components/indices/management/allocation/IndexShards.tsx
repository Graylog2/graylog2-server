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
import type { ColorVariant } from '@graylog/sawmill';

import Label from 'components/bootstrap/Label';
import Table from 'components/bootstrap/Table';

import DiagnosisSummary from './DiagnosisSummary';
import { situationTitle } from './diagnosisText';
import DisclosureToggle from './DisclosureToggle';
import { dataOn, formatTime } from './format';

import type { ShardExplanation } from '../types';

const CAN_ALLOCATE_STYLES: { [canAllocate: string]: ColorVariant } = {
  no: 'danger',
  throttled: 'warning',
  awaiting_info: 'warning',
  allocation_delayed: 'warning',
  yes: 'success',
};

export const Muted = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
  `,
);

export const Row = styled.tr<{ $open: boolean }>(
  ({ theme, $open }) => css`
    cursor: pointer;
    ${$open ? `background-color: ${theme.colors.variant.lightest.info};` : ''}
  `,
);

export const Toggle = styled.button`
  border: 0;
  background: none;
  padding: 0;
  width: 1.2em;
`;

export const DetailsCell = styled.td(
  ({ theme }) => css`
    padding: ${theme.spacings.sm} ${theme.spacings.md} ${theme.spacings.md} !important;
    border-top: 0 !important;
  `,
);

// What OpenSearch said, word for word, collapsed below the plain-language summary.
const Reported = styled.details`
  margin-bottom: 6px;
  word-break: break-word;

  pre {
    white-space: pre-wrap;
    max-height: 200px;
    overflow-y: auto;
    font-size: 0.85em;
  }
`;

const ReportedList = styled.ul`
  margin-bottom: 6px;
`;

const WhatOpenSearchReported = ({ shard }: { shard: ShardExplanation }) => {
  const nodes = shard.nodes.filter((node) => node.deciders.length > 0);

  return (
    <Reported>
      <DisclosureToggle>What OpenSearch reported</DisclosureToggle>
      <p>
        <Label bsStyle={CAN_ALLOCATE_STYLES[shard.can_allocate] ?? 'default'}>
          can allocate: {shard.can_allocate ?? 'unknown'}
        </Label>{' '}
        {shard.explanation}
      </p>
      {shard.root_cause && <p>{shard.root_cause}</p>}
      {shard.details && (
        <details>
          <DisclosureToggle>Full details</DisclosureToggle>
          <pre>{shard.details}</pre>
        </details>
      )}
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

const shardId = (shard: ShardExplanation) => `${shard.index}[${shard.shard}]${shard.primary ? 'p' : 'r'}`;

const COLUMNS = 6;

type Props = {
  shards: Array<ShardExplanation>;
  // Primary shard count of the index, for "shard 0 of 3".
  totalShards: number | undefined;
};

// The unassigned shards of one index, one line each; opening a line shows what happened in full and what can be done.
const IndexShards = ({ shards, totalShards }: Props) => {
  const [open, setOpen] = useState<string | undefined>(undefined);

  return (
    <Table condensed>
      <thead>
        <tr>
          <th aria-label="Open" />
          <th>Shard</th>
          <th>Copy</th>
          <th>What happened</th>
          <th>Data on</th>
          <th>Unassigned since</th>
        </tr>
      </thead>
      <tbody>
        {shards.map((shard) => {
          const id = shardId(shard);
          const isOpen = open === id;
          const toggle = () => setOpen(isOpen ? undefined : id);

          return (
            <React.Fragment key={id}>
              <Row $open={isOpen} onClick={toggle}>
                <td>
                  <Toggle
                    type="button"
                    aria-expanded={isOpen}
                    aria-label={`${isOpen ? 'Close' : 'Open'} ${shard.index} shard ${shard.shard}`}
                    onClick={(event) => {
                      event.stopPropagation();
                      toggle();
                    }}>
                    {isOpen ? '▾' : '▸'}
                  </Toggle>
                </td>
                <td>{shard.shard}</td>
                <td>
                  {shard.primary ? <Label bsStyle="danger">primary</Label> : <Label bsStyle="warning">replica</Label>}
                </td>
                <td>{shard.error ? 'not explained' : situationTitle(shard.diagnosis?.situation ?? null)}</td>
                <td>{dataOn(shard)}</td>
                <td>{formatTime(shard.unassigned_since)}</td>
              </Row>
              {isOpen && (
                <tr>
                  <DetailsCell colSpan={COLUMNS}>
                    {shard.error && <p>OpenSearch returned an error: {shard.error}</p>}
                    {shard.diagnosis && (
                      <DiagnosisSummary
                        diagnosis={shard.diagnosis}
                        context={{ index: shard.index, shard: shard.shard, primary: shard.primary, totalShards }}
                      />
                    )}
                    {!shard.error && <WhatOpenSearchReported shard={shard} />}
                  </DetailsCell>
                </tr>
              )}
            </React.Fragment>
          );
        })}
      </tbody>
    </Table>
  );
};

export default IndexShards;
