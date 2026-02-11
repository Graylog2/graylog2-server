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
import { useCallback } from 'react';

import useRightSidebar from 'hooks/useRightSidebar';
import type { RightSidebarContent } from 'contexts/RightSidebarContext';

type UseSidebarNavigationReturn = {
  navigateTo: <T = Record<string, unknown>>(content: RightSidebarContent<T>) => void;
  updateCurrent: <T = Record<string, unknown>>(content: RightSidebarContent<T>) => void;
  goBack: () => void;
  goForward: () => void;
  canGoBack: boolean;
  canGoForward: boolean;
};

const useSidebarNavigation = (): UseSidebarNavigationReturn => {
  const { openSidebar, updateContent, goBack, goForward, canGoBack, canGoForward } = useRightSidebar();

  const navigateTo = useCallback(
    <T = Record<string, unknown>,>(content: RightSidebarContent<T>) => {
      openSidebar(content);
    },
    [openSidebar],
  );

  const updateCurrent = useCallback(
    <T = Record<string, unknown>,>(content: RightSidebarContent<T>) => {
      updateContent(content);
    },
    [updateContent],
  );

  return {
    navigateTo,
    updateCurrent,
    goBack,
    goForward,
    canGoBack,
    canGoForward,
  };
};

export default useSidebarNavigation;
