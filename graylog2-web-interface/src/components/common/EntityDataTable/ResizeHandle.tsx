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
import styled, { css } from 'styled-components';

import Icon from 'components/common/Icon';

const ResizeButton = styled.button(
  ({ theme }) => css`
    background: transparent;
    border: 0;
    padding: 0;
    cursor: col-resize;
    color: ${theme.colors.gray[70]};
  `,
);

type Props = {
  onMouseDown?: (event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => void;
  onTouchStart?: (event: React.TouchEvent<HTMLButtonElement>) => void;
};

const ResizeHandle = ({ onMouseDown = undefined, onTouchStart = undefined }: Props) => (
  <ResizeButton onMouseDown={onMouseDown} onTouchStart={onTouchStart}>
    <Icon name="arrows_outward" />
  </ResizeButton>
);

export default ResizeHandle;
