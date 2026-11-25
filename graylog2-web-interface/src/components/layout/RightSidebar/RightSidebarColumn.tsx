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
import React, { useRef } from 'react';
import styled, { css } from 'styled-components';

import useElementDimensions from 'hooks/useElementDimensions';
import usePluginEntities from 'hooks/usePluginEntities';
import useRightSidebar from 'hooks/useRightSidebar';
import useSidebarSize from 'hooks/useSidebarSize';
import SidebarResizeHandle from 'components/common/SidebarResizeHandle';

import RightSidebar from './RightSidebar';
import { COLLAPSED_SIDEBAR_ORDER, STACKED_MIN_HEIGHT_BELOW } from './SidebarStyles';
import useGlobalSidebarState from './useGlobalSidebarState';
import SidebarWidthHandle from './SidebarWidthHandle';

const STACKED_HEIGHT = { initial: 320, min: 120, max: 640 };

// Leaves room for the global sidebar below, once the column is measured.
const stackedHeightBounds = (columnHeight: number) =>
  columnHeight > 0
    ? { ...STACKED_HEIGHT, max: Math.max(STACKED_HEIGHT.min, columnHeight - STACKED_MIN_HEIGHT_BELOW) }
    : STACKED_HEIGHT;

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

// A collapsed global sidebar needs a box of its own to move after the open sidebars.
const GlobalSidebarSlot = styled.div<{ $stacked: boolean; $collapsed: boolean }>(({ $stacked, $collapsed }) => {
  if ($stacked) {
    return css`
      flex: 1;
      min-height: 0;
      display: flex;
    `;
  }

  if ($collapsed) {
    return css`
      order: ${COLLAPSED_SIDEBAR_ORDER};
      flex-shrink: 0;
      display: flex;
    `;
  }

  return css`
    display: contents;
  `;
});

// Sidebars shown next to every page, e.g. the Assistant's chat, which stays open across pages.
const GlobalSidebars = ({ stacked }: { stacked: boolean }) => {
  const sidebars = usePluginEntities('globalSidebars');

  return (
    <>
      {sidebars.map(({ key, component: Sidebar }) => (
        <Sidebar key={key} stacked={stacked} />
      ))}
    </>
  );
};

// While a global sidebar is open and expanded next to the expanded right sidebar, both share one column, the right
// sidebar on top. Collapsed, each is a strip next to the page.
const RightSidebarColumn = () => {
  const { isOpen, isCollapsed } = useRightSidebar();
  const globalSidebar = useGlobalSidebarState();
  const columnRef = useRef<HTMLDivElement>(null);
  const { height: columnHeight } = useElementDimensions(columnRef);
  const bounds = stackedHeightBounds(columnHeight);
  const [storedHeight, setStackedHeight] = useSidebarSize('right-sidebar.stacked-height', bounds);
  // The stored height can exceed what a smaller window leaves.
  const stackedHeight = Math.min(storedHeight, bounds.max);
  const stacked = isOpen && !isCollapsed && globalSidebar.isOpen && !globalSidebar.isCollapsed;

  return (
    <Column ref={columnRef} $stacked={stacked}>
      {stacked && <SidebarWidthHandle label="Resize sidebars" />}
      {isOpen && <RightSidebar stackedHeight={stacked ? stackedHeight : undefined} />}
      {stacked && (
        <SidebarResizeHandle
          side="top"
          size={stackedHeight}
          bounds={bounds}
          label="Resize sidebar"
          onResize={setStackedHeight}
        />
      )}
      <GlobalSidebarSlot $stacked={stacked} $collapsed={globalSidebar.isCollapsed}>
        <GlobalSidebars stacked={stacked} />
      </GlobalSidebarSlot>
    </Column>
  );
};

export default RightSidebarColumn;
