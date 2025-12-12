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
import { css } from 'styled-components';

const SIZE = 5;
type Side = 'left' | 'right' | 'top' | 'bottom';

const shadowXY = (side: Side) => {
  switch (side) {
    case 'left':
      return { x: -4, y: 0 };
    case 'right':
      return { x: 4, y: 0 };
    case 'top':
      return { x: 0, y: -4 };
    case 'bottom':
      return { x: 0, y: 4 };
  }
};

const sideGeometry = (side: Side) => {
  switch (side) {
    case 'left':
      return css`
        left: 0;
        top: 0;
        bottom: 0;
        width: ${SIZE}px;
      `;
    case 'right':
      return css`
        right: 0;
        top: 0;
        bottom: 0;
        width: ${SIZE}px;
      `;
    case 'top':
      return css`
        top: 0;
        left: 0;
        right: 0;
        height: ${SIZE}px;
      `;
    case 'bottom':
      return css`
        bottom: 0;
        left: 0;
        right: 0;
        height: ${SIZE}px;
      `;
  }
};

const ScrollShadow = (side: Side) => {
  const { x, y } = shadowXY(side);

  return css`
    &::before {
      content: '';
      position: absolute;
      pointer-events: none;
      z-index: -1;
      box-shadow: ${x}px ${y}px 8px rgb(0 0 0 / 10%);
      ${sideGeometry(side)}
    }
  `;
};

export default ScrollShadow;
