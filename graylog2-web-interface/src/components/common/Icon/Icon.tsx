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

import React from 'react';
import type { RotateProp } from '@fortawesome/fontawesome-svg-core';
import styled, { css, keyframes } from 'styled-components';

import type { SizeProp, IconName } from './types';

type IconTypes = 'regular' | 'solid';

const sizeMap = {
  xs: '.938em',
  sm: '1.094em',
  lg: '1.438em',
  xl: '1.725em',
  '2x': '2.30em',
  '3x': '3.45em',
  '4x': '4.60em',
  '5x': '5.75em',
};

const spinAnimation = keyframes`
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(359deg);
  }
`;

const StyledSpan = styled.span<{
  $size: string,
  $spin: boolean,
  $rotation: RotateProp
  $flipHorizontal: boolean,
  $fill: boolean
}>(({
  $size,
  $spin,
  $rotation,
  $flipHorizontal,
  $fill,
}) => css`
  font-variation-settings: 'opsz' 48, 'wght' 700 ${$fill ? ", 'FILL' 1" : ''};
  font-size: ${sizeMap[$size] ?? '1.15em'};
  transform: rotate(${$rotation}deg) scaleY(${$flipHorizontal ? -1 : 1});
  animation: ${$spin ? css`${spinAnimation} 2s infinite linear` : 'none'};
`);

type Props = {
  className?: string,
  'data-testid'?: string,
  /** Name of Material Symbol icon */
  name: IconName,
  rotation?: 90 | 180 | 270,
  size?: SizeProp,
  spin?: boolean,
  /**
   * Name of icon type, the brand type is needed for all brand icons.
   * The type regular is needed to outlined icon.
   * Not all icons can be outlined.
   * */
  type?: IconTypes,
  style?: React.CSSProperties,
  onClick?: (event: React.MouseEvent) => void,
  onMouseEnter?: (event: React.MouseEvent) => void,
  onMouseLeave?: (event: React.MouseEvent) => void,
  onFocus?: (event: React.FocusEvent) => void,
  tabIndex?: number,
  title?: string,
  flipHorizontal?: boolean,
}

/**
 * Component that renders an icon or glyph.
 * Uses Material Symbols: https://fonts.google.com/icons
 */
const Icon = ({
  name,
  type,
  size,
  className,
  rotation,
  spin,
  flipHorizontal,
  style,
  'data-testid': testId,
  onClick,
  onMouseEnter,
  onMouseLeave,
  onFocus,
  tabIndex,
  title,
}: Props) => (
  <StyledSpan className={`material-symbols-outlined ${className ?? ''}`}
              data-testid={testId}
              onClick={onClick}
              style={style}
              title={title}
              onFocus={onFocus}
              onMouseEnter={onMouseEnter}
              onMouseLeave={onMouseLeave}
              tabIndex={tabIndex}
              $rotation={rotation}
              $flipHorizontal={flipHorizontal}
              $size={size}
              $fill={type === 'solid'}
              $spin={spin}>
    {name}
  </StyledSpan>
);

Icon.defaultProps = {
  className: undefined,
  'data-testid': undefined,
  flipHorizontal: false,
  rotation: 0,
  size: undefined,
  spin: false,
  style: undefined,
  type: 'solid',
  onClick: undefined,
  onMouseEnter: undefined,
  onMouseLeave: undefined,
  onFocus: undefined,
  tabIndex: undefined,
  title: undefined,
};

export default Icon;
