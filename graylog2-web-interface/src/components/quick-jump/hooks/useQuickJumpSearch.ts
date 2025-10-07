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
import { useState } from 'react';

import { DEFAULT_PERSPECTIVE } from 'components/perspectives/contexts/PerspectivesProvider';
import usePluginEntities from 'hooks/usePluginEntities';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';
import { PAGE_TYPE } from 'components/quick-jump/Constants';
import usePermissions from 'hooks/usePermissions';

import useRankResults from './useRankResults';

const matchesPerspective = (activePerspective: string, itemPerspective: string) =>
  activePerspective === DEFAULT_PERSPECTIVE ? !itemPerspective : itemPerspective === activePerspective;

type BaseNavigationItem = {
  description: string;
  path: string;
  permissions?: string | Array<string>;
  perspective?: string;
};

const useMainNavigationItems = () => {
  const { isPermitted } = usePermissions();
  const navigationItems = usePluginEntities('navigation');
  const { activePerspective } = useActivePerspective();

  const allNavigationItems = navigationItems.flatMap((item) =>
    'children' in item
      ? item.children.map<BaseNavigationItem>((child) => ({
          ...child,
          description: `${item.description} / ${child.description}`,
          perspective: item.perspective,
        }))
      : [item],
  );

  return allNavigationItems
    .filter((item) => isPermitted(item.permissions) && matchesPerspective(activePerspective.id, item.perspective))
    .map((item) => ({ type: PAGE_TYPE, link: item.path, title: item.description }));
};

const usePageNavigationItems = () => {
  const { isPermitted } = usePermissions();
  const pageNavigationItems = usePluginEntities('pageNavigation');

  return pageNavigationItems.flatMap((group) =>
    [...group.children]
      .filter((page) => isPermitted(page.permissions))
      .slice(1)
      .map((page) => ({ type: PAGE_TYPE, link: page.path, title: `${group.description} / ${page.description}` })),
  );
};

const useEntityCreatorItems = () => {
  const { isPermitted } = usePermissions();
  const entityCreators = usePluginEntities('entityCreators');

  return entityCreators
    .filter((creator) => (creator.permissions ? isPermitted(creator.permissions) : true))
    .map((creator) => ({ type: PAGE_TYPE, link: creator.path, title: creator.title }));
};

const useQuickJumpSearch = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const mainNavItems = useMainNavigationItems();
  const pageNavItems = usePageNavigationItems();
  const creatorItems = useEntityCreatorItems();

  const searchResults = useRankResults([...mainNavItems, ...pageNavItems, ...creatorItems], {
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
