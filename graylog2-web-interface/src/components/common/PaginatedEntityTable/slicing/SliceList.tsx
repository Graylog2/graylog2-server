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

import { Badge, ListGroup, ListGroupItem } from 'components/bootstrap';

import type { SliceRenderers, Slices } from './Slicing';

const StyledListGroup = styled(ListGroup)`
  margin-bottom: 0;
`;

const SliceInner = styled.div`
  display: flex;
  justify-content: space-between;
`;

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
          {sliceRenderers?.[sliceCol]?.render?.(slice.value) ?? slice.title ?? String(slice.value)}
          <Badge>{slice.count}</Badge>
        </SliceInner>
      </ListGroupItem>
    ))}
  </StyledListGroup>
);

export default SliceList;
