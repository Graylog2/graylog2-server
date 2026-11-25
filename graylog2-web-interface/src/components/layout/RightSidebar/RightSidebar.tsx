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
import styled, { css } from 'styled-components';

import useRightSidebar from 'hooks/useRightSidebar';
import usePluginEntities from 'hooks/usePluginEntities';
import IconButton from 'components/common/IconButton';
import SidebarResizeHandle from 'components/common/SidebarResizeHandle';

import { SIDEBAR_WIDTH, STACKED_MIN_HEIGHT_BELOW, SidebarContentArea, SidebarPanel } from './SidebarStyles';
import CollapsedSidebar from './CollapsedSidebar';
import SidebarPanelHeader from './SidebarPanelHeader';
import useRightSidebarWidth from './useRightSidebarWidth';
import useCollapseSidebars from './useCollapseSidebars';

// Stacked above a global sidebar like the Assistant, it is as wide as the column, without widening it.
const Container = styled(SidebarPanel)<{ $stackedHeight: number | undefined }>(
  ({ $stackedHeight }) => css`
    position: sticky;
    top: 0;

    ${$stackedHeight !== undefined &&
    css`
      width: auto;
      min-width: 0;
      flex-shrink: 0;
      contain: inline-size;
      animation: none;
      height: ${$stackedHeight}px;
      max-height: calc(100% - ${STACKED_MIN_HEIGHT_BELOW}px);
    `}
  `,
);

const Collapsed = styled(CollapsedSidebar)(
  ({ theme }) => css`
    border-left: 1px solid ${theme.colors.cards.border};
  `,
);

type Props = {
  // Set while it is stacked above a global sidebar like the Assistant, in one column.
  stackedHeight?: number;
};

const RightSidebar = ({ stackedHeight = undefined }: Props) => {
  const {
    content,
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
  const [width, setWidth] = useRightSidebarWidth();
  const collapseSidebars = useCollapseSidebars();

  if (!content) {
    return null;
  }

  if (isCollapsed) {
    return (
      <Collapsed
        role="complementary"
        aria-label={`${content.title} sidebar (collapsed)`}
        expandTitle="Expand sidebar"
        closeTitle="Close sidebar"
        onExpand={expandSidebar}
        onClose={closeSidebar}>
        {content.title}
      </Collapsed>
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
      $stackedHeight={stackedHeight}
      role="complementary"
      aria-label={`${content.title} sidebar`}>
      <SidebarResizeHandle
        side="right"
        width={width}
        bounds={SIDEBAR_WIDTH}
        label="Resize sidebar"
        onResize={setWidth}
        onDragPastMin={collapseSidebars}
      />
      <SidebarPanelHeader
        titleId="sidebar-title"
        title={content.title}
        leading={
          <>
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
          </>
        }
        onCollapse={collapseSidebar}
        onClose={closeSidebar}
      />
      <SidebarContentArea aria-labelledby="sidebar-title">
        <ContentComponent {...(content.props || {})} />
      </SidebarContentArea>
    </Container>
  );
};

export default RightSidebar;
