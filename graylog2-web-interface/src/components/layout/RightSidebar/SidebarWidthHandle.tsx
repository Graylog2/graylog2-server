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

import SidebarResizeHandle from 'components/common/SidebarResizeHandle';

import { SIDEBAR_WIDTH } from './SidebarStyles';
import { useSidebarLayout } from './SidebarLayoutContext';
import useRightSidebarWidth from './useRightSidebarWidth';

type Props = {
  label: string;
};

/**
 * Resizes the width the sidebars next to the page share. Dragged far past the smallest width, it collapses them all.
 */
const SidebarWidthHandle = ({ label }: Props) => {
  const [width, setWidth] = useRightSidebarWidth();
  const { collapseAll } = useSidebarLayout();

  return (
    <SidebarResizeHandle
      side="right"
      width={width}
      bounds={SIDEBAR_WIDTH}
      label={label}
      onResize={setWidth}
      onDragPastMin={collapseAll}
    />
  );
};

export default SidebarWidthHandle;
