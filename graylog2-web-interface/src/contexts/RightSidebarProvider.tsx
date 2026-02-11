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
import React, { useState, useCallback, useMemo } from 'react';

import useDisclosure from 'util/hooks/useDisclosure';
import RightSidebarContext from 'contexts/RightSidebarContext';
import type { RightSidebarContent } from 'contexts/RightSidebarContext';

type Props = {
  children: React.ReactNode;
};

const RightSidebarProvider = ({ children }: Props) => {
  const [isOpen, { open, close }] = useDisclosure(false);
  const [content, setContent] = useState<RightSidebarContent | null>(null);
  const [width, setWidthState] = useState<number>(400);

  const openSidebar = useCallback(
    <T = Record<string, unknown>>(newContent: RightSidebarContent<T>) => {
      setContent(newContent as RightSidebarContent<any>);
      open();
    },
    [open],
  );

  const closeSidebar = useCallback(() => {
    close();
  }, [close]);

  const updateContent = useCallback(
    <T = Record<string, unknown>>(newContent: RightSidebarContent<T>) => {
      setContent(newContent as RightSidebarContent<any>);
    },
    [],
  );

  const setWidth = useCallback((newWidth: number) => {
    setWidthState(newWidth);
  }, []);

  const contextValue = useMemo(
    () => ({
      isOpen,
      content,
      width,
      openSidebar,
      closeSidebar,
      updateContent,
      setWidth,
    }),
    [isOpen, content, width, openSidebar, closeSidebar, updateContent, setWidth],
  );

  return <RightSidebarContext.Provider value={contextValue}>{children}</RightSidebarContext.Provider>;
};

export default RightSidebarProvider;
