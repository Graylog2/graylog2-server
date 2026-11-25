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

type Orientation = 'horizontal' | 'vertical';

const SIZE = 36;

const fade = keyframes`
  from {
    opacity: 0;
  }
`;

// `vertical` is a strip next to the page, `horizontal` a bar sharing a column with another sidebar, taking the
// column's width without widening it.
const Container = styled.div<{ $orientation: Orientation }>(({ theme, $orientation }) =>
  $orientation === 'vertical'
    ? css`
        width: ${SIZE}px;
        min-width: ${SIZE}px;
        flex-shrink: 0;
        align-self: stretch;
        position: sticky;
        top: 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: ${theme.spacings.xs};
        background-color: ${theme.colors.global.contentBackground};
      `
    : css`
        height: ${SIZE}px;
        flex-shrink: 0;
        contain: inline-size;
        display: flex;
        flex-direction: row-reverse;
        align-items: center;
        background-color: ${theme.colors.global.contentBackground};
      `,
);

const ExpandButton = styled.button<{ $orientation: Orientation }>(
  ({ theme, $orientation }) => css`
    flex: 1;
    min-width: 0;
    min-height: 0;
    display: flex;
    align-items: center;
    justify-content: ${$orientation === 'vertical' ? 'center' : 'flex-start'};
    gap: ${theme.spacings.xs};
    ${$orientation === 'vertical' ? 'width: 100%;' : 'height: 100%;'}
    padding: 0 ${$orientation === 'vertical' ? 0 : theme.spacings.md};
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

const Title = styled.span<{ $orientation: Orientation }>(
  ({ $orientation }) => css`
    overflow: hidden;
    text-overflow: ellipsis;
    user-select: none;
    animation: ${fade} ${ANIMATION_DURATION} ease-in-out;

    ${$orientation === 'vertical' &&
    css`
      writing-mode: vertical-rl;
      text-orientation: mixed;
      transform: rotate(180deg);
      max-height: 100%;
    `}

    @media (prefers-reduced-motion: reduce) {
      animation: none;
    }
  `,
);

type Props = React.PropsWithChildren<{
  orientation: Orientation;
  // Shown next to the title, e.g. that an answer is still running.
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
 * A collapsed sidebar showing its title, which expands it on click.
 */
const CollapsedSidebar = ({
  children = undefined,
  orientation,
  status = undefined,
  expandTitle,
  closeTitle,
  onExpand,
  onClose,
  className = undefined,
  role = undefined,
  'aria-label': ariaLabel = undefined,
}: Props) => (
  <Container $orientation={orientation} className={className} role={role} aria-label={ariaLabel}>
    <IconButton name="close" title={closeTitle} onClick={onClose} />
    {orientation === 'vertical' && status}
    <ExpandButton type="button" $orientation={orientation} title={expandTitle} onClick={onExpand}>
      <Title $orientation={orientation}>{children}</Title>
      {orientation === 'horizontal' && status}
    </ExpandButton>
  </Container>
);

export default CollapsedSidebar;
