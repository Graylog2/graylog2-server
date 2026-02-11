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

export type RightSidebarContent = {
  id: string;
  title: string;
  component: React.ComponentType<any>;
  props?: Record<string, any>;
};

export type RightSidebarContextType = {
  isOpen: boolean;
  content: RightSidebarContent | null;
  width: number;
  openSidebar: (content: RightSidebarContent) => void;
  closeSidebar: () => void;
  updateContent: (content: RightSidebarContent) => void;
  setWidth: (width: number) => void;
};

const RightSidebarContext = React.createContext<RightSidebarContextType | undefined>(undefined);

export default singleton('contexts.RightSidebar', () => RightSidebarContext);
