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
import React, { createContext, useCallback, useMemo, useState } from 'react';

import type { SidebarContextType } from '../utils/types';

export const SidebarContext = createContext<SidebarContextType | null>(null);

export const SidebarProvider = ({ children = undefined }: React.PropsWithChildren<{}>) => {
  const [sidebar, setSidebar] = useState<React.ReactElement | null>(null);

  const clearSidebar = useCallback(() => {
    setSidebar(null);
  }, []);

  const sidebarProvider = useMemo(() => ({ sidebar, clearSidebar, setSidebar }), [sidebar, clearSidebar, setSidebar]);

  return <SidebarContext.Provider value={sidebarProvider}>{children}</SidebarContext.Provider>;
};
