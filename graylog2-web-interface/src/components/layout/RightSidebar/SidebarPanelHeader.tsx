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

import IconButton from 'components/common/IconButton';

const Header = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-shrink: 0;
    align-items: center;
    gap: ${theme.spacings.xs};
    padding: ${theme.spacings.sm} ${theme.spacings.md};
    border-bottom: 1px solid ${theme.colors.cards.border};
  `,
);

const Title = styled.h2(
  ({ theme }) => css`
    flex: 1;
    min-width: 0;
    margin: 0;
    font-size: ${theme.fonts.size.h3};
    font-weight: 600;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  `,
);

type Props = React.PropsWithChildren<{
  title: React.ReactNode;
  titleId?: string;
  // Before the title, e.g. an icon or navigation buttons.
  leading?: React.ReactNode;
  collapseTitle?: string;
  onCollapse?: () => void;
  closeTitle?: string;
  onClose?: () => void;
}>;

/**
 * The header of a sidebar next to the page: its title, its own actions as children, then collapse and close.
 */
const SidebarPanelHeader = ({
  title,
  titleId = undefined,
  leading = undefined,
  children = undefined,
  collapseTitle = 'Collapse sidebar',
  onCollapse = undefined,
  closeTitle = 'Close sidebar',
  onClose = undefined,
}: Props) => (
  <Header>
    {leading}
    <Title id={titleId}>{title}</Title>
    {children}
    {onCollapse && <IconButton name="right_panel_close" title={collapseTitle} onClick={onCollapse} />}
    {onClose && <IconButton name="close" title={closeTitle} onClick={onClose} />}
  </Header>
);

export default SidebarPanelHeader;
