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
import styled from 'styled-components';

import Alert from 'components/bootstrap/Alert';
import Button from 'components/bootstrap/Button';
import Popover from 'components/common/Popover';
import Spinner from 'components/common/Spinner';

import type { IndexGroup } from './mapLayout';
import ShardGlyph from './ShardGlyph';

import DiagnosisSummary from '../DiagnosisSummary';
import { useShardExplanation } from '../useAllocation';
import type { MapCopy } from '../../types';

const Details = styled.div`
  max-width: 520px;
`;

export const copyId = (copy: MapCopy) => `${copy.index}[${copy.shard}]${copy.primary ? 'p' : 'r'}`;

type DetailsProps = {
  copy: MapCopy;
  totalShards: number | undefined;
  onShowIndex: (index: string) => void;
};

// The pop-up of one copy. Only an unassigned copy is explained, and only once it is opened.
const CopyDetails = ({ copy, totalShards, onShowIndex }: DetailsProps) => {
  const unassigned = copy.state === 'UNASSIGNED';
  const { explanation, error, isLoading } = useShardExplanation(unassigned ? copy : undefined);

  return (
    <Details>
      {unassigned && isLoading && <Spinner text="Asking OpenSearch..." />}
      {error && <Alert bsStyle="danger">Couldn&apos;t explain this shard: {error.message}</Alert>}
      {explanation?.diagnosis && (
        <DiagnosisSummary
          diagnosis={explanation.diagnosis}
          context={{ index: copy.index, shard: copy.shard, primary: copy.primary, totalShards }}
        />
      )}
      {!unassigned && (
        <p>
          On {copy.node}: {copy.state === 'STARTED' ? 'healthy' : copy.state.toLowerCase()}.
        </p>
      )}
      <Button bsSize="xsmall" onClick={() => onShowIndex(copy.index)}>
        Show {copy.index} in the list
      </Button>
    </Details>
  );
};

// What an index gets from the map.
export type IndexViewProps = {
  group: IndexGroup;
  expanded: boolean;
  onToggleExpanded: () => void;
  selected: string | undefined;
  onSelect: (id: string | undefined) => void;
  shardCounts: { [index: string]: number };
  onShowIndex: (index: string) => void;
};

type Props = Pick<IndexViewProps, 'selected' | 'onSelect' | 'shardCounts' | 'onShowIndex'> & {
  copy: MapCopy;
};

// One shard copy; the open one carries the pop-up with its diagnosis.
const CopyGlyph = ({ copy, selected, onSelect, shardCounts, onShowIndex }: Props) => {
  const id = copyId(copy);

  if (id !== selected) {
    return <ShardGlyph copy={copy} selected={false} onClick={() => onSelect(id)} />;
  }

  return (
    <Popover opened onClose={() => onSelect(undefined)} position="bottom" withArrow shadow="md">
      <Popover.Target>
        <ShardGlyph copy={copy} selected onClick={() => onSelect(undefined)} />
      </Popover.Target>
      <Popover.Dropdown
        title={`${copy.index}, shard ${copy.shard} (${copy.primary ? 'primary' : 'replica'})`}
        id={`shard-${id}`}>
        <CopyDetails copy={copy} totalShards={shardCounts[copy.index]} onShowIndex={onShowIndex} />
      </Popover.Dropdown>
    </Popover>
  );
};

export default CopyGlyph;
