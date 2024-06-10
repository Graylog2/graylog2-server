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

import styled, { css, keyframes } from 'styled-components';

const animateIncrease = keyframes`
  0% {
    width: 0;
  }
  100% {
    width: 100%;
  }
`;

const animateDecrease = keyframes`
  0% {
    width: 100%;
  }
  100% {
    width: 0;
  }
`;

const ProgressAnimation = styled.div<{ $animationDuration: number, $increase: boolean }>(({ theme, $animationDuration, $increase }) => css`
  position: absolute;
  top: 0;
  bottom: 0;
  height: 2px;
  animation: linear ${$increase ? animateIncrease : animateDecrease} ${$animationDuration}ms;
  background-color: ${theme.colors.global.textDefault};
  z-index: 2;
  ${$increase ? 'left: 0;' : 'right: 0;'} 
`);

export default ProgressAnimation;
