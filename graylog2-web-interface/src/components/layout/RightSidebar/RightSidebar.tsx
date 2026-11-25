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
import React, { useState } from 'react';
import styled, { css } from 'styled-components';

import useRightSidebar from 'hooks/useRightSidebar';
import usePluginEntities from 'hooks/usePluginEntities';
import IconButton from 'components/common/IconButton';

import { COLLAPSED_SIDEBAR_ORDER, STACKED_MIN_HEIGHT_BELOW, SidebarContentArea, SidebarPanel } from './SidebarStyles';
import CollapsedSidebar from './CollapsedSidebar';
import SidebarPanelHeader from './SidebarPanelHeader';
import useRightSidebarWidth from './useRightSidebarWidth';
import SidebarWidthHandle from './SidebarWidthHandle';

// Stacked above a global sidebar like the Assistant, it is as wide as the column, without widening it.
const Container = styled(SidebarPanel)<{ $stackedHeight: number | undefined; $slidesIn: boolean }>(
  ({ $stackedHeight, $slidesIn }) => css`
    position: sticky;
    top: 0;
    ${!$slidesIn &&
    css`
      animation: none;
    `}

    ${$stackedHeight !== undefined &&
    css`
      width: auto;
      min-width: 0;
      flex-shrink: 0;
      contain: inline-size;
      height: ${$stackedHeight}px;
      max-height: calc(100% - ${STACKED_MIN_HEIGHT_BELOW}px);
    `}
  `,
);

type PanelProps = React.PropsWithChildren<{
  width: number;
  slidesIn: boolean;
  stackedHeight: number | undefined;
  title: string;
}>;

// Decides when it shows whether it slides in: only next to the page, as the column's width cannot be animated, and not
// when a page opened it while loading.
// Deciding once keeps switching between stacked and side by side from replaying the slide, which looked like the
// sidebar closing and opening again.
const Panel = ({ width, slidesIn, stackedHeight, title, children = undefined }: PanelProps) => {
  const [slides] = useState(slidesIn && stackedHeight === undefined);

  return (
    <Container
      $width={width}
      $stackedHeight={stackedHeight}
      $slidesIn={slides}
      role="complementary"
      aria-label={`${title} sidebar`}>
      {children}
    </Container>
  );
};

const Collapsed = styled(CollapsedSidebar)(
  ({ theme }) => css`
    order: ${COLLAPSED_SIDEBAR_ORDER};
    border-left: 1px solid ${theme.colors.cards.border};
  `,
);

type Props = {
  // Set while it is stacked above a global sidebar like the Assistant, in one column, which has the resize handle.
  stackedHeight?: number;
};

const RightSidebar = ({ stackedHeight = undefined }: Props) => {
  const {
    content,
    isCollapsed,
    slidesIn,
    closeSidebar,
    collapseSidebar,
    expandSidebar,
    goBack,
    goForward,
    canGoBack,
    canGoForward,
  } = useRightSidebar();
  const sidebarComponents = usePluginEntities('sidebar.components');
  const [width] = useRightSidebarWidth();

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
    <Panel width={width} slidesIn={slidesIn} stackedHeight={stackedHeight} title={content.title}>
      {stackedHeight === undefined && <SidebarWidthHandle label="Resize sidebar" />}
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
    </Panel>
  );
};

export default RightSidebar;
