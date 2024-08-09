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
import { Drawer as MantineDrawer } from '@mantine/core';
import styled, { css } from 'styled-components';

const StyledDrawer = styled(MantineDrawer)(({ theme }) => css`
  .mantine-Drawer-content, .mantine-Drawer-header {
    background-color: ${theme.colors.global.contentBackground};
  }

  .mantine-Drawer-content {
    display: flex;
    flex-direction: column;
  }

  .mantine-Drawer-body {
    flex: 1;
  }
`);

const Title = styled.div(({ theme }) => css`
  font-size: ${theme.fonts.size.h1};
`);
type Props = Pick<React.ComponentProps<typeof MantineDrawer>, 'opened' | 'onClose' | 'position' | 'size' | 'children' | 'title' | 'closeOnClickOutside'>;
const Drawer = ({ title, ...props }: Props) => <StyledDrawer offset={15} padding="lg" radius={5} zIndex={1032} title={<Title>{title}</Title>} {...props} />;

export default Drawer;
