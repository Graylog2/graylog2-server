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

import useRightSidebar from 'hooks/useRightSidebar';
import usePluginEntities from 'hooks/usePluginEntities';
import Icon from 'components/common/Icon';
import IconButton from 'components/common/IconButton';

import { ANIMATION_DURATION, SidebarContainer, SidebarRow, SidebarContentArea, SidebarHeader, SidebarTitle } from './SidebarStyles';

const COLLAPSED_WIDTH = 36;

const fade = keyframes`
  from {
    opacity: 0;
  }
`;

const Container = styled(SidebarContainer)`
  position: sticky;
  top: 0;
`;

const CollapsedContainer = styled.div(
  ({ theme }) => css`
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
  `,
);

const CollapsedTitleArea = styled.div(
  ({ theme }) => css`
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    cursor: pointer;
    overflow: hidden;

    :hover {
      background-color: ${theme.colors.variant.lightest.default};
    }
  `,
);

const CollapsedTitle = styled.span(
  ({ theme }) => css`
    writing-mode: vertical-rl;
    text-orientation: mixed;
    transform: rotate(180deg);
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

const RightSidebar = () => {
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

  if (!content) {
    return null;
  }

  if (isCollapsed) {
    return (
      <CollapsedContainer role="complementary" aria-label={`${content.title} sidebar (collapsed)`}>
        <IconButton name="close" title="Close sidebar" onClick={closeSidebar} aria-label="Close sidebar" />
        <CollapsedTitleArea onClick={expandSidebar} title="Expand sidebar">
          <CollapsedTitle>{content.title}</CollapsedTitle>
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
    <Container $width={width} role="complementary" aria-label={`${content.title} sidebar`}>
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
