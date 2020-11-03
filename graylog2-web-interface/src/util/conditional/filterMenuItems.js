// @flow strict

import AppConfig from '../AppConfig';

function isAllowedPath(
  path: string,
  toExclude: Array<string>,
): boolean {
  return toExclude.indexOf(path) === -1;
}

type MenuItem = { path: string };

function filterMenuItems(
  menuItems: Array<MenuItem>,
  toExclude: Array<string>,
): Array<MenuItem> {
  return menuItems.filter((item) => isAllowedPath(item.path, toExclude));
}

export function filterCloudMenuItems(
  menuItems: Array<MenuItem>,
  toExclude: Array<string>,
): Array<MenuItem> {
  if (!AppConfig.isCloud()) {
    return menuItems;
  }

  return filterMenuItems(menuItems, toExclude);
}

export default filterMenuItems;
