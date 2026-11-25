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
import React from 'react';
import styled, { css, keyframes } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

import useRightSidebar from 'hooks/useRightSidebar';
import usePluginEntities from 'hooks/usePluginEntities';
import useSidebarWidth from 'hooks/useSidebarWidth';
import Icon from 'components/common/Icon';
import IconButton from 'components/common/IconButton';
import SidebarResizeHandle from 'components/common/SidebarResizeHandle';

import {
  ANIMATION_DURATION,
  SidebarContainer,
  SidebarRow,
  SidebarContentArea,
  SidebarHeader,
  SidebarTitle,
} from './SidebarStyles';

const COLLAPSED_WIDTH = 36;
const STACKED_HEIGHT = { initial: 320, min: 120, max: 640 };
// Keeps the Assistant below usable however far the sidebar is dragged down.
const MIN_HEIGHT_BELOW = 240;

const fade = keyframes`
  from {
    opacity: 0;
  }
`;

// Stacked above a global sidebar like the Assistant: as wide as the column, without widening it.
const stackedStyles = (theme: DefaultTheme) => css`
  width: auto;
  min-width: 0;
  flex-shrink: 0;
  contain: inline-size;
  background-color: ${theme.colors.global.contentBackground};
  border-left: 1px solid ${theme.colors.cards.border};
  border-bottom: 1px solid ${theme.colors.cards.border};
`;

const Container = styled(SidebarContainer)<{ $stackedHeight: number | undefined }>(
  ({ theme, $stackedHeight }) => css`
    position: sticky;
    top: 0;

    ${$stackedHeight !== undefined &&
    css`
      ${stackedStyles(theme)};
      height: ${$stackedHeight}px;
      max-height: calc(100% - ${MIN_HEIGHT_BELOW}px);
      animation: none;

      /* Keeps the resize handle on its bottom edge above the global sidebar. */
      z-index: 1;
    `}
  `,
);

const CollapsedContainer = styled.div<{ $stacked: boolean }>(
  ({ theme, $stacked }) => css`
    width: ${COLLAPSED_WIDTH}px;
    min-width: ${COLLAPSED_WIDTH}px;
    flex-shrink: 0;
    align-self: stretch;
    position: sticky;
    top: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    background-color: ${theme.colors.global.contentBackground};

    ${$stacked &&
    css`
      ${stackedStyles(theme)};
      height: ${COLLAPSED_WIDTH}px;
      flex-direction: row-reverse;
    `}
  `,
);

const CollapsedTitleArea = styled.div<{ $stacked: boolean }>(
  ({ theme, $stacked }) => css`
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: ${$stacked ? 'flex-start' : 'center'};
    width: 100%;
    height: 100%;
    padding: 0 ${$stacked ? theme.spacings.md : 0};
    cursor: pointer;
    overflow: hidden;

    :hover {
      background-color: ${theme.colors.variant.lightest.default};
    }
  `,
);

const CollapsedTitle = styled.span<{ $stacked: boolean }>(
  ({ theme, $stacked }) => css`
    writing-mode: ${$stacked ? 'horizontal-tb' : 'vertical-rl'};
    text-orientation: mixed;
    transform: ${$stacked ? 'none' : 'rotate(180deg)'};
    font-size: ${theme.fonts.size.small};
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-height: 100%;
    color: ${theme.colors.text.primary};
    user-select: none;

    animation: ${fade} ${ANIMATION_DURATION} ease-in-out;

    @media (prefers-reduced-motion: reduce) {
      animation: none;
    }
  `,
);

const NavigationButtons = styled.div`
  display: flex;
  gap: 4px;
  margin-right: 8px;
`;

const HeaderActions = styled.div`
  display: flex;
  align-items: center;
  gap: 4px;
`;

const CollapseButton = styled.button(
  ({ theme }) => css`
    background: none;
    border: none;
    padding: 4px;
    cursor: pointer;
    color: ${theme.colors.text.primary};
    display: flex;
    align-items: center;

    :hover {
      color: ${theme.colors.variant.dark.default};
    }
  `,
);

type Props = {
  // Above a global sidebar like the Assistant, in one column.
  stacked?: boolean;
};

const RightSidebar = ({ stacked = false }: Props) => {
  const {
    content,
    width,
    isCollapsed,
    closeSidebar,
    collapseSidebar,
    expandSidebar,
    goBack,
    goForward,
    canGoBack,
    canGoForward,
  } = useRightSidebar();
  const sidebarComponents = usePluginEntities('sidebar.components');
  const [stackedHeight, setStackedHeight] = useSidebarWidth('right-sidebar.stacked-height', STACKED_HEIGHT);

  if (!content) {
    return null;
  }

  if (isCollapsed) {
    return (
      <CollapsedContainer $stacked={stacked} role="complementary" aria-label={`${content.title} sidebar (collapsed)`}>
        <IconButton name="close" title="Close sidebar" onClick={closeSidebar} aria-label="Close sidebar" />
        <CollapsedTitleArea $stacked={stacked} onClick={expandSidebar} title="Expand sidebar">
          <CollapsedTitle $stacked={stacked}>{content.title}</CollapsedTitle>
        </CollapsedTitleArea>
      </CollapsedContainer>
    );
  }

  const ContentComponent =
    content.component ?? sidebarComponents.find((c) => c.key === content.componentKey)?.component;

  if (!ContentComponent) {
    return null;
  }

  return (
    <Container
      $width={width}
      $stackedHeight={stacked ? stackedHeight : undefined}
      role="complementary"
      aria-label={`${content.title} sidebar`}>
      {stacked && (
        <SidebarResizeHandle
          side="top"
          width={stackedHeight}
          bounds={STACKED_HEIGHT}
          label="Resize sidebar"
          onResize={setStackedHeight}
        />
      )}
      <SidebarRow className="content">
        <SidebarHeader>
          <NavigationButtons>
            <IconButton
              name="arrow_back"
              title="Go back"
              onClick={goBack}
              disabled={!canGoBack}
              aria-label="Go back to previous content"
            />
            <IconButton
              name="arrow_forward"
              title="Go forward"
              onClick={goForward}
              disabled={!canGoForward}
              aria-label="Go forward to next content"
            />
          </NavigationButtons>
          <SidebarTitle id="sidebar-title">{content.title}</SidebarTitle>
          <HeaderActions>
            <CollapseButton
              type="button"
              title="Collapse sidebar"
              onClick={collapseSidebar}
              aria-label="Collapse sidebar">
              <Icon name="collapse_content" />
            </CollapseButton>
            <IconButton name="close" title="Close sidebar" onClick={closeSidebar} aria-label="Close sidebar" />
          </HeaderActions>
        </SidebarHeader>
        <SidebarContentArea aria-labelledby="sidebar-title">
          <ContentComponent {...(content.props || {})} />
        </SidebarContentArea>
      </SidebarRow>
    </Container>
  );
};

export default RightSidebar;
