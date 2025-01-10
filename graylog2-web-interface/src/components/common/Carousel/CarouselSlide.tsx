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
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';

export interface CarouselSlideProps extends React.ComponentPropsWithoutRef<'div'> {
  children?: React.ReactNode;
  className?: string,
  gap?: number;
  size?: string | number;
}

const StyledSlide = styled.div<{ $size?: string | number, $gap?: number }>(({ $size, $gap, theme }: {
  theme: DefaultTheme,
  $size: string | number,
  $gap: number
}) => css`
  flex: 0 0 ${$size ?? '24%'};
  min-width: 0;
  min-height: 100px;
  margin-right: ${$gap ?? theme.spacings.sm};
  position: relative;
`);

const CarouselSlide = ({ children, size, gap, className }: CarouselSlideProps) => (
  <StyledSlide $size={size} $gap={gap} className={className}>
    {children}
  </StyledSlide>
);

export default CarouselSlide;
