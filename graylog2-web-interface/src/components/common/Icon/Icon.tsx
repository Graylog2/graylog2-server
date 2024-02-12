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
import styled, { css } from 'styled-components';

import type { IconName, SizeProp } from './types';

type IconTypes = 'brand' | 'regular' | 'solid';

const sizeMap = {
  xs: '.75em',
  sm: '.875em',
  lg: '1.25em',
  xl: '1.5em',
  '2x': '2em',
  '3x': '3em',
  '4x': '4em',
  '5x': '5em',
};

const StyledSpan = styled.span<{ $size: string }>(({ theme, $size }) => css`
  font-variation-settings: 'opsz' 48, 'wght' 700;
  font-size: ${sizeMap[$size] ?? 'inherit'};
`);

type Props = {
  className?: string,
  'data-testid'?: string,
  /** Name of Material Symbol icon */
  name: IconName,
  rotation?: RotateProp,
  size?: SizeProp,
  spin?: boolean,
  /**
   * Name of icon type, the brand type is needed for all brand icons.
   * The type regular is needed to outlined icon.
   * Not all icons can be outlined.
   * */
  type?: IconTypes,
  fixedWidth?: boolean,
  inverse?: boolean,
  style?: React.CSSProperties,
  onClick?: (event: React.MouseEvent<SVGSVGElement>) => void,
  onMouseEnter?: (event: React.MouseEvent<SVGSVGElement>) => void,
  onMouseLeave?: (event: React.MouseEvent<SVGSVGElement>) => void,
  onFocus?: (event: React.FocusEvent<SVGSVGElement>) => void,
  tabIndex?: number,
  title?: string,
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
  fixedWidth,
  inverse,
  style,
  'data-testid': testId,
  onClick,
  onMouseEnter,
  onMouseLeave,
  onFocus,
  tabIndex,
  title,
}: Props) => {
  return (
    <StyledSpan className={`material-symbols-outlined ${className}`}
                data-testid={testId}
                $size={size}
                style={style}>
      {name}
    </StyledSpan>
  );

  return (
    <CustomFontAwesomeIcon className={className}
                           data-testid={testId}
                           fixedWidth={fixedWidth}
                           icon={{ prefix, iconName }}
                           inverse={inverse}
                           rotation={rotation}
                           size={size}
                           spin={spin}
                           style={style}
                           onClick={onClick}
                           onMouseEnter={onMouseEnter}
                           onMouseLeave={onMouseLeave}
                           tabIndex={tabIndex}
                           title={title}
                           onFocus={onFocus} />
  );
};

Icon.defaultProps = {
  className: undefined,
  'data-testid': undefined,
  fixedWidth: false,
  inverse: false,
  rotation: undefined,
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
