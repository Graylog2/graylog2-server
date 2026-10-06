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
import { useCallback } from 'react';
import styled, { css } from 'styled-components';

import SegmentedControl from 'components/bootstrap/SegmentedControl';
import useLayoutVariant from 'components/common/PaginatedEntityTable/hooks/useLayoutVariant';

import { STREAM_VIEW_VARIANTS } from './Constants';

const Container = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
  `,
);

const Label = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
    white-space: nowrap;
  `,
);

const NarrowSegmentedControl = styled(SegmentedControl)`
  width: fit-content;
`;

const SEGMENTS = [
  { label: 'Default', value: STREAM_VIEW_VARIANTS.default },
  { label: 'Routing', value: STREAM_VIEW_VARIANTS.routing },
  { label: 'Performance', value: STREAM_VIEW_VARIANTS.performance },
];

type VariantValue = (typeof STREAM_VIEW_VARIANTS)[keyof typeof STREAM_VIEW_VARIANTS];

const StreamViewButtons = () => {
  const { activeLayoutVariant, selectLayoutVariant } = useLayoutVariant();

  const onChange = useCallback(
    (value: VariantValue) => {
      if (value === STREAM_VIEW_VARIANTS.default) {
        if (activeLayoutVariant) selectLayoutVariant(activeLayoutVariant);
      } else {
        selectLayoutVariant(value);
      }
    },
    [activeLayoutVariant, selectLayoutVariant],
  );

  return (
    <Container>
      <Label>View:</Label>
      <NarrowSegmentedControl<VariantValue>
        data={SEGMENTS}
        value={activeLayoutVariant as VariantValue}
        onChange={onChange}
      />
    </Container>
  );
};

export default StreamViewButtons;
