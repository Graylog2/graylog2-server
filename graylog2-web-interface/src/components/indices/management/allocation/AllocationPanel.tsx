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

import Alert from 'components/bootstrap/Alert';
import Button from 'components/bootstrap/Button';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import Label from 'components/bootstrap/Label';
import Table from 'components/bootstrap/Table';
import Spinner from 'components/common/Spinner';

import { useAllocationExplain } from './useAllocation';

import type { AllocationExplanation, ShardExplanation } from '../types';

const CAN_ALLOCATE_STYLES: { [canAllocate: string]: ColorVariant } = {
  no: 'danger',
  throttled: 'warning',
  awaiting_info: 'warning',
  allocation_delayed: 'warning',
  yes: 'success',
};

const Section = styled.section(
  ({ theme }) => css`
    border: 1px solid ${theme.colors.gray[80]};
    border-radius: 4px;
    padding: 12px 15px;
    margin-bottom: 20px;
  `,
);

const Header = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;

  h3 {
    margin: 0;
  }
`;

const HeaderEnd = styled(ButtonToolbar)`
  margin-left: auto;
`;

const Muted = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
  `,
);

const Group = styled.div(
  ({ theme }) => css`
    border-top: 1px solid ${theme.colors.gray[90]};
    padding-top: 10px;
    margin-top: 10px;
  `,
);

const GroupTitle = styled.p`
  font-weight: bold;
  margin-bottom: 6px;
`;

const IndexLink = styled(Button)`
  padding: 0;
  font-family: monospace;
`;

const RootCause = styled.td`
  max-width: 480px;
  word-break: break-word;

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

const NodeList = styled.ul`
  margin-bottom: 0;
`;

const formatTime = (iso: string | null) => (iso ? new Date(iso).toLocaleString() : '');
const plural = (count: number, singular: string, pluralForm: string) =>
  `${count} ${count === 1 ? singular : pluralForm}`;

// Shards with the same answer (verdict, explanation, NO deciders per node) are shown once.
const groupKey = (shard: ShardExplanation) =>
  JSON.stringify([
    shard.error ? `error:${shard.error}` : shard.can_allocate,
    shard.explanation,
    shard.nodes.map((node) => [node.node_name, node.deciders.map((d) => [d.decider, d.explanation])]),
  ]);

const groupShards = (shards: Array<ShardExplanation>) => {
  const groups = new Map<string, Array<ShardExplanation>>();
  shards.forEach((shard) => {
    const key = groupKey(shard);
    groups.set(key, [...(groups.get(key) ?? []), shard]);
  });

  return [...groups.values()];
};

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
        <th>Unassigned because</th>
        <th>Since</th>
        <th>Failed attempts</th>
        <th>Root cause</th>
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
          <td>{shard.unassigned_reason}</td>
          <td>{formatTime(shard.unassigned_since)}</td>
          <td>{shard.failed_attempts ?? ''}</td>
          <RootCause>
            {shard.error ?? shard.root_cause ?? ''}
            {shard.details && (
              <details>
                <summary>Full details</summary>
                <pre>{shard.details}</pre>
              </details>
            )}
          </RootCause>
        </tr>
      ))}
    </tbody>
  </Table>
);

const ExplanationGroup = ({ shards, onShowIndex }: ShardsProps) => {
  const [first] = shards;

  return (
    <Group>
      <GroupTitle>
        {first.error ? (
          <Label bsStyle="default">not explained</Label>
        ) : (
          <Label bsStyle={CAN_ALLOCATE_STYLES[first.can_allocate] ?? 'default'}>
            can allocate: {first.can_allocate ?? 'unknown'}
          </Label>
        )}{' '}
        {plural(shards.length, 'shard', 'shards')}: {first.error ? 'OpenSearch returned an error' : first.explanation}
      </GroupTitle>
      <ShardTable shards={shards} onShowIndex={onShowIndex} />
      {first.nodes.some((node) => node.deciders.length > 0) && (
        <>
          <Muted>Why, per node (only the checks that said no):</Muted>
          <NodeList>
            {first.nodes
              .filter((node) => node.deciders.length > 0)
              .map((node) => (
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
          </NodeList>
        </>
      )}
    </Group>
  );
};

const Summary = ({ explanation }: { explanation: AllocationExplanation }) => {
  const replicas = explanation.unassigned_total - explanation.unassigned_primaries;

  return (
    <p>
      <strong>{plural(explanation.unassigned_total, 'unassigned shard', 'unassigned shards')}</strong>
      {': '}
      {plural(explanation.unassigned_primaries, 'primary', 'primaries')} (their indices are red),{' '}
      {plural(replicas, 'replica', 'replicas')} (yellow).
      {explanation.truncated && ` Showing the first ${explanation.explained.length}.`}
    </p>
  );
};

type BodyProps = {
  explanation: AllocationExplanation;
  onShowIndex: (index: string) => void;
  onRetry: (() => void) | undefined;
};

const AllocationBody = ({ explanation, onShowIndex, onRetry }: BodyProps) => {
  const groups = groupShards(explanation.explained);
  const maxRetriesExceeded = explanation.explained.some((shard) => shard.max_retries_exceeded);

  if (explanation.unassigned_total === 0) {
    return <Alert bsStyle="success">No unassigned shards: every shard is allocated.</Alert>;
  }

  return (
    <>
      <Summary explanation={explanation} />
      {maxRetriesExceeded && (
        <Alert bsStyle="info">
          OpenSearch has stopped retrying some of these shards.{' '}
          {onRetry ? (
            <Button bsSize="xsmall" onClick={onRetry}>
              Retry failed allocations
            </Button>
          ) : (
            'Retrying failed allocations'
          )}{' '}
          asks it to try again. That helps if the cause was temporary; if the root cause is still there (corrupted
          files, for example) they will fail again, and restoring or deleting the index is the way out.
        </Alert>
      )}
      {groups.map((shards) => (
        <ExplanationGroup key={groupKey(shards[0])} shards={shards} onShowIndex={onShowIndex} />
      ))}
    </>
  );
};

type Props = {
  onClose: () => void;
  onShowIndex: (index: string) => void;
  // Without it (the user may not retry), the hint names the action instead of offering a button.
  onRetry?: () => void;
};

const AllocationPanel = ({ onClose, onShowIndex, onRetry = undefined }: Props) => {
  const { explanation, error, isFetching, refetch } = useAllocationExplain(true);

  return (
    <Section aria-label="Shard allocation">
      <Header>
        <h3>Shard allocation</h3>
        {explanation && !isFetching && <Muted>as of {formatTime(explanation.generated_at)}</Muted>}
        <HeaderEnd>
          <Button bsSize="small" onClick={() => refetch()} disabled={isFetching}>
            {isFetching ? 'Explaining...' : 'Explain again'}
          </Button>
          <Button bsSize="small" onClick={onClose}>
            Close
          </Button>
        </HeaderEnd>
      </Header>
      {isFetching && !explanation && <Spinner text="Asking OpenSearch about unassigned shards..." />}
      {error && <Alert bsStyle="danger">Couldn&apos;t explain shard allocation: {error.message}</Alert>}
      {explanation && <AllocationBody explanation={explanation} onShowIndex={onShowIndex} onRetry={onRetry} />}
    </Section>
  );
};

export default AllocationPanel;
