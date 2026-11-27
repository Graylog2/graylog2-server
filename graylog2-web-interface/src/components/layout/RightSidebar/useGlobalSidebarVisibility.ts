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
import usePluginEntities from 'hooks/usePluginEntities';

// The plugin entities are fixed for the lifetime of the page, so the hooks are always called in the same order.
const useGlobalSidebarVisibility = () => {
  const visibilities = usePluginEntities('globalSidebars').map((sidebar) => sidebar.useVisibility?.() ?? 'closed');

  if (visibilities.includes('collapsed')) {
    return 'collapsed';
  }

  return visibilities.includes('expanded') ? 'expanded' : 'closed';
};

export default useGlobalSidebarVisibility;
