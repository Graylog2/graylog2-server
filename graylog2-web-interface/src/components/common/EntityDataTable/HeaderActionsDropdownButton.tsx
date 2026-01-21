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

import Menu from 'components/bootstrap/Menu';
import Icon from 'components/common/Icon';

type Props = {
  children: React.ReactNode;
  label: string;
  onChangeSlicing: () => void;
};

const DropdownTrigger = styled.button(
  ({ theme }) => css`
    background: transparent;
    border: 0;
    padding: 0;
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
    color: inherit;
    font: inherit;
    cursor: pointer;
    line-height: normal;

    &:focus-visible {
      outline-offset: 2px;
    }
  `,
);

const HeaderActionsDropdownButton = ({ children, label, onChangeSlicing }: Props) => (
  <Menu shadow="md" withinPortal position="bottom-start">
    <Menu.Target>
      <DropdownTrigger type="button" title={`Toggle ${label} actions`} aria-label={`Toggle ${label} actions`}>
        <span>{children}</span>
        <Icon name="arrow_drop_down" size="xs" />
      </DropdownTrigger>
    </Menu.Target>
    <Menu.Dropdown>
      <Menu.Item onClick={onChangeSlicing}>Slice by values</Menu.Item>
    </Menu.Dropdown>
  </Menu>
);

export default HeaderActionsDropdownButton;
