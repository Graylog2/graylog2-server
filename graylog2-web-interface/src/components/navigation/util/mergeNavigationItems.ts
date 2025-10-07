import type { PluginNavigation } from 'graylog-web-plugin';

const _existingDropdownItemIndex = (
  existingNavigationItems: Array<PluginNavigation>,
  newNavigationItem: PluginNavigation,
) => {
  if (!newNavigationItem.children) {
    return -1;
  }

  return existingNavigationItems.findIndex(
    ({ description, perspective, children }) =>
      newNavigationItem.description === description && newNavigationItem.perspective === perspective && children,
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

export default mergeNavigationItems;
