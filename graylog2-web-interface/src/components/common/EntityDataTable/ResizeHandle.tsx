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
    margin-left: ${theme.spacings.xxs};
    user-select: none;
  }
    
  `,
);

type Props = {
  onMouseDown?: (event: React.MouseEvent<HTMLDivElement, MouseEvent>) => void;
  onTouchStart?: (event: React.TouchEvent<HTMLDivElement>) => void;
  colTitle: string;
};

const ResizeHandle = ({ onMouseDown = undefined, onTouchStart = undefined, colTitle }: Props) => {
  const _onMouseDown = (e) => {
    // Required to prevent text selection while resizing in Safari
    e?.preventDefault();
    onMouseDown?.(e);
  };

  const _onTouchStart = (e) => {
    e?.preventDefault();
    onTouchStart?.(e);
  };

  return (
    <Container
      onMouseDown={_onMouseDown}
      onTouchStart={_onTouchStart}
      role="separator"
      aria-label={`Resize ${colTitle} column`}
      title={`Resize ${colTitle} column`}>
      <Icon name="arrows_outward" />
    </Container>
  );
};

export default ResizeHandle;
