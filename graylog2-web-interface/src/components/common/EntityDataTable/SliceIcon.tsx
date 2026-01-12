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
import { useState } from 'react';
import styled, { css } from 'styled-components';

import { MenuItem } from 'components/bootstrap';
import IconButton from 'components/common/IconButton';
import OverlayDropdown from 'components/common/OverlayDropdown';

const StyledIconButton = styled(IconButton)(
  ({ theme }) => css`
    display: inline-block;
    margin-left: ${theme.spacings.xs};
    padding: 0;
    cursor: pointer;
  `,
);

type Props = {
  onClick: (event: React.MouseEvent<HTMLDivElement>) => void;
};

const SliceIcon = ({ onClick }: Props) => (
  <OverlayDropdown
    show={show}
    onClose={onClose}
    toggleChild={
      <div className={`dropdown btn-group ${show ? 'open' : ''}`}>
        <StyledIconButton name="more_horiz" title="Toggle column actions" />
      </div>
    }
    placement="bottom-start"
    onToggle={_onToggle}>
    <MenuItem onClick={onClick}>Slice by values</MenuItem>
  </OverlayDropdown>
);

export default SliceIcon;
