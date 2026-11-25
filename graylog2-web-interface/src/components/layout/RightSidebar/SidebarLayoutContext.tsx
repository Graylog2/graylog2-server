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
import { useContext } from 'react';

import { singleton } from 'logic/singleton';

type SidebarLayout = {
  // Whether the right sidebar and a global sidebar like the Assistant share one column.
  stacked: boolean;
  // Collapses every open sidebar next to the page at once.
  collapseAll: () => void;
};

// Set by the column the sidebars next to the page are in, so they follow one layout.
const SidebarLayoutContext = singleton('contexts.SidebarLayout', () =>
  React.createContext<SidebarLayout>({ stacked: false, collapseAll: () => {} }),
);

export const useSidebarLayout = () => useContext(SidebarLayoutContext);

export default SidebarLayoutContext;
