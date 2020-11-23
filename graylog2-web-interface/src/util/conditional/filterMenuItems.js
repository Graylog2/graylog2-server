// @flow strict

import AppConfig from '../AppConfig';

type MenuItem = { path: string };

function filterMenuItems(
  menuItems: Array<MenuItem>,
  toExclude: Array<string>,
): Array<MenuItem> {
  return menuItems.filter((item) => !toExclude.includes(item.path));
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
