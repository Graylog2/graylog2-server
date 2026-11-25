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

import usePluginEntities from 'hooks/usePluginEntities';
import useRightSidebar from 'hooks/useRightSidebar';
import useSidebarWidth from 'hooks/useSidebarWidth';
import SidebarResizeHandle from 'components/common/SidebarResizeHandle';

import RightSidebar from './RightSidebar';
import useGlobalSidebarState from './useGlobalSidebarState';

const STACKED_HEIGHT = { initial: 320, min: 120, max: 640 };

// Has no box of its own while the sidebars sit side by side; the same elements in both layouts keep the global
// sidebars mounted, e.g. so a running answer of the Assistant finishes.
const Column = styled.div<{ $stacked: boolean }>(({ $stacked }) =>
  $stacked
    ? css`
        position: sticky;
        top: 0;
        flex-shrink: 0;
        align-self: stretch;
        display: flex;
        flex-direction: column;
        min-height: 0;
      `
    : css`
        display: contents;
      `,
);

// A collapsed global sidebar only takes the height of its bar.
const GlobalSidebarSlot = styled.div<{ $stacked: boolean; $collapsed: boolean }>(({ $stacked, $collapsed }) => {
  if (!$stacked) {
    return css`
      display: contents;
    `;
  }

  return css`
    flex: ${$collapsed ? '0 0 auto' : '1'};
    min-height: 0;
    display: flex;
  `;
});

// Sidebars shown next to every page, e.g. the Assistant's chat, which stays open across pages.
const GlobalSidebars = () => {
  const sidebars = usePluginEntities('globalSidebars');

  return (
    <>
      {sidebars.map(({ key, component: Sidebar }) => (
        <Sidebar key={key} />
      ))}
    </>
  );
};

// While a global sidebar is open next to the right sidebar, both share one column, the right sidebar on top. Only one
// of them is collapsed at a time; the global sidebar takes care of that.
const RightSidebarColumn = () => {
  const { isOpen, isCollapsed } = useRightSidebar();
  const globalSidebar = useGlobalSidebarState();
  const [stackedHeight, setStackedHeight] = useSidebarWidth('right-sidebar.stacked-height', STACKED_HEIGHT);
  const stacked = isOpen && globalSidebar.isOpen;
  const isGlobalSidebarCollapsed = stacked && globalSidebar.isCollapsed;
  const detailsHeight = isGlobalSidebarCollapsed ? 'fill' : stackedHeight;

  return (
    <Column $stacked={stacked}>
      {isOpen && <RightSidebar stackedHeight={stacked ? detailsHeight : undefined} />}
      {stacked && !isCollapsed && !isGlobalSidebarCollapsed && (
        <SidebarResizeHandle
          variant="bar"
          side="top"
          width={stackedHeight}
          bounds={STACKED_HEIGHT}
          label="Resize sidebar"
          onResize={setStackedHeight}
        />
      )}
      <GlobalSidebarSlot $stacked={stacked} $collapsed={isGlobalSidebarCollapsed}>
        <GlobalSidebars />
      </GlobalSidebarSlot>
    </Column>
  );
};

export default RightSidebarColumn;
