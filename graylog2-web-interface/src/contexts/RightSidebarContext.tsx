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

import { singleton } from 'logic/singleton';

export type RightSidebarContent<T = Record<string, unknown>> = {
  id: string;
  title: string;
  component: React.ComponentType<T>;
  props?: T;
};

export type RightSidebarContextType = {
  isOpen: boolean;
  content: RightSidebarContent<any> | null;
  width: number;
  openSidebar: <T = Record<string, unknown>>(content: RightSidebarContent<T>) => void;
  closeSidebar: () => void;
  updateContent: <T = Record<string, unknown>>(content: RightSidebarContent<T>) => void;
  setWidth: (width: number) => void;
  goBack: () => void;
  goForward: () => void;
  canGoBack: boolean;
  canGoForward: boolean;
};

const RightSidebarContext = React.createContext<RightSidebarContextType | undefined>(undefined);

export default singleton('contexts.RightSidebar', () => RightSidebarContext);
