import { useState } from 'react';

import { DEFAULT_PERSPECTIVE } from 'components/perspectives/contexts/PerspectivesProvider';
import usePluginEntities from 'hooks/usePluginEntities';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';
import { PAGE_TYPE } from 'components/quick-jump/Constants';
import usePermissions from 'hooks/usePermissions';

import useRankResults from './useRankResults';

const matchesPerspective = (activePerspective: string, itemPerspective: string) =>
  activePerspective === DEFAULT_PERSPECTIVE ? !itemPerspective : itemPerspective === activePerspective;

const useMainNavigationItems = () => {
  const { isPermitted } = usePermissions();
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
      if (!isPermitted(item.permissions)) {
        return false;
      }

      if (!matchesPerspective(activePerspective.id, item.perspective)) {
        return false;
      }

      return true;
    })
    .map((item) => ({ type: PAGE_TYPE, link: item.path, title: item.description }));
};

const useQuickJumpSearch = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const allNavItems = useMainNavigationItems();

  const searchResults = useRankResults(allNavItems, {
    query: searchQuery,
    categoryWeights: { page: 0.9, entity: 1.0 },
    minRelevance: 0.35,
  });

  return {
    searchQuery,
    searchResults,
    setSearchQuery,
  };
};

export default useQuickJumpSearch;
