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
import styled, { css, keyframes } from 'styled-components';

import IconButton from 'components/common/IconButton';

import { ANIMATION_DURATION } from './SidebarStyles';

const WIDTH = 36;

const fade = keyframes`
  from {
    opacity: 0;
  }
`;

const Container = styled.div(
  ({ theme }) => css`
    width: ${WIDTH}px;
    min-width: ${WIDTH}px;
    flex-shrink: 0;
    align-self: stretch;
    position: sticky;
    top: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: ${theme.spacings.xs};
    background-color: ${theme.colors.global.contentBackground};
  `,
);

const ExpandButton = styled.button(
  ({ theme }) => css`
    flex: 1;
    min-height: 0;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
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

const Title = styled.span`
  writing-mode: vertical-rl;
  text-orientation: mixed;
  transform: rotate(180deg);
  max-height: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  user-select: none;
  animation: ${fade} ${ANIMATION_DURATION} ease-in-out;

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;

type Props = React.PropsWithChildren<{
  // Shown above the title, e.g. that an answer is still running.
  status?: React.ReactNode;
  expandTitle: string;
  closeTitle: string;
  onExpand: () => void;
  onClose: () => void;
  className?: string;
  role?: React.AriaRole;
  'aria-label'?: string;
}>;

/**
 * A collapsed sidebar: a strip next to the page showing its title, which expands it on click.
 */
const CollapsedSidebar = ({
  children = undefined,
  status = undefined,
  expandTitle,
  closeTitle,
  onExpand,
  onClose,
  className = undefined,
  role = undefined,
  'aria-label': ariaLabel = undefined,
}: Props) => (
  <Container className={className} role={role} aria-label={ariaLabel}>
    <IconButton name="close" title={closeTitle} onClick={onClose} />
    {status}
    <ExpandButton type="button" title={expandTitle} onClick={onExpand}>
      <Title>{children}</Title>
    </ExpandButton>
  </Container>
);

export default CollapsedSidebar;
