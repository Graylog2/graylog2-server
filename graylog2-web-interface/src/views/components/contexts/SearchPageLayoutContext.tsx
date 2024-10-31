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

export const SAVE_COPY = {
  save: { isShown: false },
  saveAs: { isShown: true },
  share: { isShown: false },
  actionsDropdown: { isShown: false },
};

export const BLANK = {
  save: { isShown: false },
  saveAs: { isShown: false },
  share: { isShown: false },
  actionsDropdown: { isShown: false },
};

export type LayoutState = {
  sidebar: { isShown: boolean },
  viewActions: {
    save: { isShown: boolean },
    saveAs: { isShown: boolean },
    share: { isShown: boolean },
    actionsDropdown: { isShown: boolean },
  },
  searchAreaContainer?: { component: React.ComponentType }
  infoBar?: { component: React.ComponentType }
}

export const DEFAULT_STATE: LayoutState = {
  sidebar: { isShown: true },
  viewActions: {
    save: { isShown: true },
    saveAs: { isShown: true },
    share: { isShown: true },
    actionsDropdown: { isShown: true },
  },
};

const SearchPageLayoutContext = React.createContext<LayoutState>(DEFAULT_STATE);

export default singleton('contexts.SearchPageLayout', () => SearchPageLayoutContext);
