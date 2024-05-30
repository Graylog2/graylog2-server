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

const animateProgress = keyframes`
  0% {
    width: 0;
  }
  100% {
    width: 100%;
  }
`;
const ProgressAnimation = styled.div<{ $animationDuration: number }>(({ theme, $animationDuration }) => css`
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  animation: linear ${animateProgress} ${$animationDuration}ms;
  background-color: ${theme.colors.gray['80']};
  z-index: 0;
`);

export default ProgressAnimation;
