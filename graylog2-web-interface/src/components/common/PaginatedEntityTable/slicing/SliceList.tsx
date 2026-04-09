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

import { ListGroup, ListGroupItem } from 'components/bootstrap';

// import { formatReadableNumber } from 'util/NumberFormatting';
import type { SliceRenderers, Slices } from './Slicing';

const StyledListGroup = styled(ListGroup)`
  margin-bottom: 0;
`;

const SliceInner = styled.div`
  display: flex;
  justify-content: space-between;
  gap: 2px;
`;

const Title = styled.div`
  word-break: break-word;
  flex: 1;
`;

const Additional = styled.div`
  white-space: nowrap;
  display: flex;
  align-items: flex-start;
  gap: 2px;
`;

/*
const CountBadge = styled(Badge)`
  overflow: visible;

  .mantine-Badge-label {
    overflow: visible;
  }
`;
*/

type Props = {
  slices: Slices;
  activeSlice: string | undefined;
  sliceCol: string | undefined;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string | undefined) => void;
  sliceRenderers?: SliceRenderers;
  keyPrefix?: string;
  listTestId?: string;
};

const SliceList = ({
  slices,
  activeSlice,
  sliceCol,
  onChangeSlicing,
  sliceRenderers = undefined,
  keyPrefix = '',
  listTestId = undefined,
}: Props) => (
  <StyledListGroup data-testid={listTestId}>
    {slices.map((slice) => (
      <ListGroupItem
        key={`${keyPrefix}${String(slice.value)}`}
        onClick={() => onChangeSlicing(sliceCol, String(slice.value))}
        active={String(activeSlice) === String(slice.value)}>
        <SliceInner>
          <Title>{sliceRenderers?.[sliceCol]?.render?.(slice) ?? slice.title ?? String(slice.value)}</Title>
          {sliceRenderers?.[sliceCol]?.renderAdditional ? (
            <Additional>{sliceRenderers?.[sliceCol]?.renderAdditional?.(slice)}</Additional>
          ) : null}
        </SliceInner>
      </ListGroupItem>
    ))}
  </StyledListGroup>
);

export default SliceList;
