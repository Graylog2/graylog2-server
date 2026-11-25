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
import useRightSidebar from 'hooks/useRightSidebar';

import useGlobalSidebarState from './useGlobalSidebarState';

// Whether the right sidebar and a global sidebar like the Assistant share one column, which they do while both are
// open and expanded.
const useAreSidebarsStacked = () => {
  const { isOpen, isCollapsed } = useRightSidebar();
  const globalSidebar = useGlobalSidebarState();

  return isOpen && !isCollapsed && globalSidebar.isOpen && !globalSidebar.isCollapsed;
};

export default useAreSidebarsStacked;
