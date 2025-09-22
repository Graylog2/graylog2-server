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

import React, { forwardRef } from 'react';
import styled, { css, keyframes } from 'styled-components';

import type { SizeProp, RotateProp, IconName, FlipProp, IconType } from './types';

const sizeMap = {
  xs: '.938em',
  sm: '1.094em',
  lg: '1.438em',
  xl: '1.725em',
  '2x': '2.30em',
  '3x': '3.45em',
  '4x': '4.60em',
  '5x': '5.75em',
  'huge': '10.35em',
};

const spinAnimation = keyframes`
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(359deg);
  }
`;

type ColorVariants = 'success' | 'warning';

const StyledSpan = styled.span<{
  $size: string;
  $spin: boolean;
  $rotation: RotateProp;
  $flip: FlipProp;
  $fill: boolean;
  $bsStyle: ColorVariants | undefined;
}>(
  ({ $bsStyle, $size, $spin, $rotation, $flip, $fill, theme }) => css`
    font-variation-settings:
      'opsz' 48,
      'wght' 700 ${$fill ? ", 'FILL' 1" : ''};
    font-size: ${sizeMap[$size] ?? '1.15em'};
    transform: rotate(${$rotation}deg) scaleY(${$flip === 'horizontal' || $flip === 'both' ? -1 : 1})
      scaleX(${$flip === 'vertical' || $flip === 'both' ? -1 : 1});
    animation: ${$spin
      ? css`
          ${spinAnimation} 2s infinite linear
        `
      : 'none'};
    vertical-align: middle;
    color: ${$bsStyle ? theme.colors.button[$bsStyle].background : 'inherit'};
  `,
);

type Props = {
  bsStyle?: ColorVariants; // if this prop is not defined, the inherited font color will be used.
  className?: string;
  'data-testid'?: string;
  /** Name of Material Symbol icon */
  name: IconName;
  rotation?: RotateProp;
  size?: SizeProp;
  spin?: boolean;
  /**
   * Name of icon type, the brand type is needed for all brand icons.
   * The type regular is needed to outlined icon.
   * Not all icons can be outlined.
   * */
  type?: IconType;
  style?: React.CSSProperties;
  onClick?: (event: React.MouseEvent) => void;
  onMouseEnter?: (event: React.MouseEvent) => void;
  onMouseLeave?: (event: React.MouseEvent) => void;
  onFocus?: (event: React.FocusEvent) => void;
  tabIndex?: number;
  title?: string;
  flip?: FlipProp;
};

/**
 * Component that renders an icon or glyph.
 * Uses Material Symbols: https://fonts.google.com/icons
 * Have a look at the `BrandIcon` component for brand icons.
 */
const Icon = (
  {
    bsStyle = undefined,
    name,
    type = 'solid',
    size = undefined,
    className = undefined,
    rotation = 0,
    spin = false,
    flip = undefined,
    style = undefined,
    'data-testid': testId = undefined,
    onClick = undefined,
    onMouseEnter = undefined,
    onMouseLeave = undefined,
    onFocus = undefined,
    tabIndex = undefined,
    title = undefined,
  }: Props,
  ref: React.ForwardedRef<HTMLSpanElement>,
) => (
  <StyledSpan
    className={`material-symbols-rounded ${className ?? ''}`}
    data-testid={testId}
    onClick={onClick}
    style={style}
    title={title}
    onFocus={onFocus}
    onMouseEnter={onMouseEnter}
    onMouseLeave={onMouseLeave}
    tabIndex={tabIndex}
    ref={ref}
    $bsStyle={bsStyle}
    $rotation={rotation}
    $flip={flip}
    $size={size}
    $fill={type === 'solid'}
    $spin={spin}>
    {name}
  </StyledSpan>
);

export default forwardRef(Icon);
