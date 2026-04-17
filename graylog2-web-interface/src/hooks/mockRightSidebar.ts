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
import type { RightSidebarContextType } from 'contexts/RightSidebarContext';

const mockRightSidebar = (): RightSidebarContextType => ({
  isOpen: false,
  isCollapsed: false,
  content: null,
  width: 400,
  openSidebar: jest.fn(),
  closeSidebar: jest.fn(),
  collapseSidebar: jest.fn(),
  expandSidebar: jest.fn(),
  updateContent: jest.fn(),
  setWidth: jest.fn(),
  goBack: jest.fn(),
  goForward: jest.fn(),
  canGoBack: false,
  canGoForward: false,
});

export default mockRightSidebar;
