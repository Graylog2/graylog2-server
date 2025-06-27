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
