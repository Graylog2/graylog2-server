import type * as Immutable from 'immutable';
import type React from 'react';
import { useState } from 'react';

import { DEFAULT_PERSPECTIVE } from 'components/perspectives/contexts/PerspectivesProvider';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import usePluginEntities from 'hooks/usePluginEntities';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';

const matchesPerspective = (activePerspective: string, itemPerspective: string) =>
  activePerspective === DEFAULT_PERSPECTIVE ? !itemPerspective : itemPerspective === activePerspective;

const matchesPermission = (userPermissions: Immutable.List<string>, itemPermissions: Array<string>) =>
  isPermitted(userPermissions, itemPermissions);

const useMainNavigationItems = () => {
  const currentUser = useCurrentUser();
  const allNavigationItems = usePluginEntities('navigation') as any;
  const { activePerspective } = useActivePerspective();
  const navigationLinks = allNavigationItems.filter((item) => !item.children);
  const dropdownLinks = allNavigationItems
    .filter((item) => item.children)
    .flatMap((item) =>
      item.children.map((child) => ({
        ...child,
        description: `${item.description} / ${child.description}`,
        perspective: item.perspective,
      })),
    );

  return [...navigationLinks, ...dropdownLinks]
    .filter((item) => {
      if (!matchesPermission(currentUser.permissions, item.permissions)) {
        return false;
      }

      if (!matchesPerspective(activePerspective.id, item.perspective)) {
        return false;
      }

      return true;
    })
    .map((item) => ({ type: 'page', link: item.path, title: item.description }));
};

const useQuickJumpSearch = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const allNavItems = useMainNavigationItems();

  // {
  //   type: 'page'
  //   link: '/search'
  //   title: 'Search'
  // }

  const handleSearch = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(event.target.value);
  };

  return {
    searchQuery,
    searchResults: allNavItems,
    setSearchQuery: handleSearch,
  };
};

export default useQuickJumpSearch;
