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

const HEIGHT = 36;

// Takes the column's width without widening it.
const Bar = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    flex-shrink: 0;
    height: ${HEIGHT}px;
    contain: inline-size;
    background-color: ${theme.colors.global.contentBackground};
  `,
);

const ExpandButton = styled.button(
  ({ theme }) => css`
    flex: 1;
    min-width: 0;
    height: 100%;
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
    padding: 0 ${theme.spacings.md};
    background: none;
    border: 0;
    color: ${theme.colors.text.primary};
    font-size: ${theme.fonts.size.small};
    white-space: nowrap;
    overflow: hidden;
    cursor: pointer;

    :hover {
      background-color: ${theme.colors.variant.lightest.default};
    }
  `,
);

type Props = React.PropsWithChildren<{
  expandTitle: string;
  closeTitle: string;
  onExpand: () => void;
  onClose: () => void;
  className?: string;
  role?: React.AriaRole;
  'aria-label'?: string;
}>;

/**
 * A collapsed sidebar that shares a column with another one: a bar showing its title, which expands it on click.
 */
const CollapsedSidebarBar = ({
  children = undefined,
  expandTitle,
  closeTitle,
  onExpand,
  onClose,
  className = undefined,
  role = undefined,
  'aria-label': ariaLabel = undefined,
}: Props) => (
  <Bar className={className} role={role} aria-label={ariaLabel}>
    <ExpandButton type="button" title={expandTitle} onClick={onExpand}>
      {children}
    </ExpandButton>
    <IconButton name="close" title={closeTitle} onClick={onClose} />
  </Bar>
);

export default CollapsedSidebarBar;
