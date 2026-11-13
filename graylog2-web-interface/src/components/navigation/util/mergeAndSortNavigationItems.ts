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
import type { PluginNavigation } from 'graylog-web-plugin';

import sortNavigationItems from './sortNavigationItems';

const _existingDropdownItemIndex = (
  existingNavigationItems: Array<PluginNavigation>,
  newNavigationItem: PluginNavigation,
) => {
  if (!newNavigationItem.children) {
    return -1;
  }

  return existingNavigationItems.findIndex(
    ({ description, children }) => newNavigationItem.description === description && children,
  );
};

const mergeNavigationItems = (navigationItems: Array<PluginNavigation>): Array<PluginNavigation> =>
  navigationItems.reduce((result, current) => {
    const existingDropdownItemIndex = _existingDropdownItemIndex(result, current);

    if (existingDropdownItemIndex >= 0) {
      const existingDropdownItem = result[existingDropdownItemIndex];
      const newDropdownItem = {
        ...current,
        ...existingDropdownItem,
        children: [...existingDropdownItem.children, ...current.children],
      };
      const newResult = [...result];
      newResult[existingDropdownItemIndex] = newDropdownItem;

      return newResult;
    }

    return [...result, current];
  }, []);

// Sorting only once after merging, because sorting multiple times changes the order of items positioned after the same item.
const mergeAndSortNavigationItems = (navigationItems: Array<PluginNavigation>): Array<PluginNavigation> =>
  mergeNavigationItems(navigationItems).map((item) =>
    'children' in item ? { ...item, children: sortNavigationItems(item.children) } : item,
  );

export default mergeAndSortNavigationItems;
