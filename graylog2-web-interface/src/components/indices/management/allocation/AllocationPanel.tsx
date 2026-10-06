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
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import SegmentedControl from 'components/bootstrap/SegmentedControl';
import Spinner from 'components/common/Spinner';

import { formatTime, plural } from './format';
import AllocationMap from './map/AllocationMap';
import ShardList from './ShardList';
import { useAllocationExplain, useShardMap } from './useAllocation';

import type { AllocationExplanation } from '../types';

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
  align-items: center;
`;

const Muted = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
  `,
);

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
  picked: Set<string>;
  onTogglePicked: (index: string) => void;
  onRetry: (() => void) | undefined;
  shardCounts: { [index: string]: number };
};

const AllocationBody = ({ explanation, picked, onTogglePicked, onRetry, shardCounts }: BodyProps) => {
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
          asks it to try again. That helps when the cause was temporary; open a shard below to see whether it does.
        </Alert>
      )}
      <ShardList shards={explanation.explained} picked={picked} onTogglePicked={onTogglePicked} shardCounts={shardCounts} />
    </>
  );
};

type Props = {
  onClose: () => void;
  picked: Set<string>;
  onTogglePicked: (index: string) => void;
  // Without it (the user may not retry), the hint names the action instead of offering a button.
  onRetry?: () => void;
  // Primary shard count per index, for "shard 0 of 3".
  shardCounts?: { [index: string]: number };
};

type View = 'map' | 'list';

const VIEWS: Array<{ value: View; label: string }> = [
  { value: 'map', label: 'Map' },
  { value: 'list', label: 'List' },
];

// The map needs two cheap calls and explains one shard when opened; the list explains up to 50 shards at once.
const AllocationPanel = ({ onClose, picked, onTogglePicked, onRetry = undefined, shardCounts = {} }: Props) => {
  const [view, setView] = useState<View>('map');
  const list = useAllocationExplain(view === 'list');
  const shardMap = useShardMap(view === 'map');
  const isFetching = view === 'map' ? shardMap.isFetching : list.isFetching;
  const error = view === 'map' ? shardMap.error : list.error;
  const generatedAt = view === 'map' ? shardMap.map?.generated_at : list.explanation?.generated_at;

  return (
    <Section aria-label="Shard allocation">
      <Header>
        <h3>Shard allocation</h3>
        {generatedAt && !isFetching && <Muted>as of {formatTime(generatedAt)}</Muted>}
        <HeaderEnd>
          <SegmentedControl<View> data={VIEWS} value={view} onChange={setView} />
          <Button
            bsSize="small"
            onClick={() => (view === 'map' ? shardMap.refetch() : list.refetch())}
            disabled={isFetching}>
            {isFetching ? 'Explaining...' : 'Explain again'}
          </Button>
          <Button bsSize="small" onClick={onClose}>
            Close
          </Button>
        </HeaderEnd>
      </Header>
      {isFetching && !generatedAt && <Spinner text="Asking OpenSearch about shards..." />}
      {error && <Alert bsStyle="danger">Couldn&apos;t explain shard allocation: {error.message}</Alert>}
      {view === 'map' && shardMap.map && (
        <AllocationMap map={shardMap.map} shardCounts={shardCounts} picked={picked} onTogglePicked={onTogglePicked} />
      )}
      {view === 'list' && list.explanation && (
        <AllocationBody
          explanation={list.explanation}
          picked={picked}
          onTogglePicked={onTogglePicked}
          onRetry={onRetry}
          shardCounts={shardCounts}
        />
      )}
    </Section>
  );
};

export default AllocationPanel;
