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
import styled, { css, keyframes } from 'styled-components';

import Row from 'components/bootstrap/Row';

const ANIMATION_DURATION = '0.3s';
// Keeps a global sidebar like the Assistant usable below the stacked right sidebar.
const STACKED_MIN_HEIGHT_BELOW = 240;
// Moves collapsed sidebars after the open ones, so their strips are always on the page's right edge.
const COLLAPSED_SIDEBAR_ORDER = 1;
// Widths a user can resize a sidebar next to the page to.
const SIDEBAR_WIDTH = { initial: 400, min: 320, max: 760 };

const slideAnimation = keyframes`
  from {
    width: 0;
    min-width: 0;
  }
`;

const fadeAnimation = keyframes`
  from {
    opacity: 0;
  }
`;

const SidebarContainer = styled.div<{ $width: number }>(
  ({ $width }) => css`
    width: ${$width}px;
    min-width: ${$width}px;
    flex-shrink: 0;
    align-self: stretch;
    display: flex;
    flex-direction: column;
    padding: 5px;
    animation: ${slideAnimation} ${ANIMATION_DURATION} ease-in-out;

    @media (prefers-reduced-motion: reduce) {
      animation: none;
    }
  `,
);

// A sidebar next to the page with its own background, e.g. the details sidebar or the Assistant.
const SidebarPanel = styled(SidebarContainer)(
  ({ theme }) => css`
    position: relative;
    overflow: hidden;
    padding: 0;
    background-color: ${theme.colors.global.contentBackground};
    border-left: 1px solid ${theme.colors.cards.border};
  `,
);

const SidebarRow = styled(Row)`
  margin-left: 0;
  margin-right: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
`;

const SidebarContentArea = styled.div`
  flex: 1;
  overflow: hidden auto;
  padding: 15px;
  min-height: 0;

  animation: ${fadeAnimation} ${ANIMATION_DURATION} cubic-bezier(1, 0, 1, 0.3);

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;

const SidebarHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 15px;
  padding-right: 15px;
`;

const SidebarTitle = styled.h4(
  ({ theme }) => css`
    margin: 0;
    font-size: ${theme.fonts.size.h1};
    font-weight: normal;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
  `,
);

export {
  ANIMATION_DURATION,
  STACKED_MIN_HEIGHT_BELOW,
  COLLAPSED_SIDEBAR_ORDER,
  SIDEBAR_WIDTH,
  slideAnimation,
  fadeAnimation,
  SidebarContainer,
  SidebarPanel,
  SidebarRow,
  SidebarContentArea,
  SidebarHeader,
  SidebarTitle,
};
