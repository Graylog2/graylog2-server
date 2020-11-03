// @flow strict

import AppConfig from '../AppConfig';

type MenuItem = { path: string };

function filterMenuItems(
  menuItems: Array<MenuItem>,
  toFilter: Array<string>,
  inUse: boolean = true,
): Array<MenuItem> {
  // skip filtering if not in use
  if (!inUse) {
    return menuItems;
  }

  return menuItems.filter((item) => toFilter.indexOf(item.path) === -1);
}

export function filterCloudMenuItem(menuItems: Array<MenuItem>, toFilter: Array<string>): Array<MenuItem> {
  return filterMenuItems(menuItems, toFilter, AppConfig.isCloud());
}

export default filterMenuItems;
