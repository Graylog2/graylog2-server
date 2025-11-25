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

const Container = styled.div(
  ({ theme }) => css`
    cursor: col-resize;
    color: ${theme.colors.gray[70]};
  `,
);

type Props = {
  onMouseDown?: (event: React.MouseEvent<HTMLDivElement, MouseEvent>) => void;
  onTouchStart?: (event: React.TouchEvent<HTMLDivElement>) => void;
};

const ResizeHandle = ({ onMouseDown = undefined, onTouchStart = undefined }: Props) => (
  <Container onMouseDown={onMouseDown} onTouchStart={onTouchStart} role="separator">
    <Icon name="arrows_outward" />
  </Container>
);

export default ResizeHandle;
