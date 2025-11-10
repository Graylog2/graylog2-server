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
import styled from 'styled-components';
import type { ThemeBase } from '@graylog/sawmill';

export type FlexBaseType = {
  $gap?: (typeof ThemeBase)['spacings'][0] | number;
  $justify?: 'flex-start' | 'flex-end' | 'center' | 'space-between' | 'space-around' | 'space-evenly';
  $width?: string;
  $wrap?: boolean;
};

export type FlexItemType = FlexBaseType & {
  $align?: 'stretch' | 'flex-start' | 'flex-end' | 'center' | 'baseline';
};

export type FlexContainerType = FlexBaseType & {
  $direction: 'column' | 'row';
  $alignContent?:
    | 'normal'
    | 'flex-start'
    | 'flex-end'
    | 'center'
    | 'space-between'
    | 'space-around'
    | 'space-evenly'
    | 'stretch';
};

const FlexBase = styled.div<FlexBaseType>`
  display: flex;
  gap: ${({ theme, $gap }) => (Number.isFinite($gap) ? `${$gap}rem` : theme.spacings[$gap]) || theme.spacings.sm};
  justify-content: ${({ $justify }) => $justify || 'flex-start'};
  flex-wrap: ${({ $wrap }) => ($wrap ? 'wrap' : 'nowrap')};
  width: ${({ $width }) => $width || '100%'};

  @media (width <= 991px) {
    flex-direction: column;
  }
`;

export const Container = styled(FlexBase)<FlexContainerType>`
  flex-direction: ${({ $direction }) => $direction};
  align-content: ${({ $alignContent }) => $alignContent || 'normal'};
`;

export const Row = styled(FlexBase)<FlexItemType>`
  flex-direction: row;
  align-items: ${({ $align }) => $align || 'flex-start'};
`;

export const Col = styled(FlexBase)<FlexItemType>`
  flex-direction: column;
  align-items: ${({ $align }) => $align || 'flex-start'};
`;
