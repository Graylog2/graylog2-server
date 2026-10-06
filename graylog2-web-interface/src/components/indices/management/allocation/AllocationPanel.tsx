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

import Alert from 'components/bootstrap/Alert';
import Button from 'components/bootstrap/Button';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import Spinner from 'components/common/Spinner';

import ExplanationGroup from './ExplanationGroup';
import { formatTime, plural } from './format';
import { useAllocationExplain } from './useAllocation';

import type { AllocationExplanation, ShardExplanation, Situation } from '../types';

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

// Problems with one particular copy are told per shard; rule-based ones (disk, filters, ...) can cover many.
const PER_COPY: Array<Situation> = [
  'TRANSIENT_FAILURE',
  'RESTORE_FAILED',
  'TRANSLOG_DAMAGED',
  'RETENTION_LEASES_DAMAGED',
  'SEGMENT_DATA_DAMAGED',
  'COMMIT_UNREADABLE',
  'DAMAGED_OTHER',
  'STALE_COPY_ONLY',
  'NO_COPY_FOUND',
  'REPLICA_REBUILDS_FROM_PRIMARY',
];

// Shards with the same answer (situation, verdict, explanation, NO deciders per node) are shown once.
const groupKey = (shard: ShardExplanation) =>
  JSON.stringify([
    shard.diagnosis?.situation,
    PER_COPY.includes(shard.diagnosis?.situation) ? `${shard.index}[${shard.shard}]` : null,
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
  shardCounts: { [index: string]: number };
};

const AllocationBody = ({ explanation, onShowIndex, onRetry, shardCounts }: BodyProps) => {
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
          asks it to try again. That helps when the cause was temporary; each shard below says whether it does.
        </Alert>
      )}
      {groups.map((shards) => (
        <ExplanationGroup
          key={groupKey(shards[0])}
          shards={shards}
          onShowIndex={onShowIndex}
          shardCounts={shardCounts}
        />
      ))}
    </>
  );
};

type Props = {
  onClose: () => void;
  onShowIndex: (index: string) => void;
  // Without it (the user may not retry), the hint names the action instead of offering a button.
  onRetry?: () => void;
  // Primary shard count per index, for "shard 0 of 3".
  shardCounts?: { [index: string]: number };
};

const AllocationPanel = ({ onClose, onShowIndex, onRetry = undefined, shardCounts = {} }: Props) => {
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
      {explanation && (
        <AllocationBody
          explanation={explanation}
          onShowIndex={onShowIndex}
          onRetry={onRetry}
          shardCounts={shardCounts}
        />
      )}
    </Section>
  );
};

export default AllocationPanel;
