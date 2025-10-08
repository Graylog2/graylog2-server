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
import { useMemo, useState } from 'react';

import { DEFAULT_PERSPECTIVE } from 'components/perspectives/contexts/PerspectivesProvider';
import usePluginEntities from 'hooks/usePluginEntities';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';
import { PAGE_TYPE, PAGE_WEIGHT, BASE_SCORE } from 'components/quick-jump/Constants';
import usePermissions from 'hooks/usePermissions';
import type { QualifiedUrl } from 'routing/Routes';
import Routes, { prefixUrl } from 'routing/Routes';
import AppConfig from 'util/AppConfig';

import useEntitySearchResults from './useEntitySearchResults';

const matchesPerspective = (activePerspective: string, itemPerspective: string) =>
  activePerspective === DEFAULT_PERSPECTIVE ? !itemPerspective : itemPerspective === activePerspective;

const isFeatureEnabled = (featureFlag?: string) => {
  if (!featureFlag) return true;

  return AppConfig.isFeatureEnabled(featureFlag);
};

type BaseNavigationItem = {
  description: string;
  path: QualifiedUrl<string>;
  permissions?: string | Array<string>;
  perspective?: string;
};

type SearchResultItem = {
  key?: string;
  type: string;
  link: QualifiedUrl<string>;
  title: string;
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
  const { activePerspective } = useActivePerspective();
  const { isPermitted } = usePermissions();
  const pageNavigationItems = usePluginEntities('pageNavigation');

  return pageNavigationItems
    .filter((group) => matchesPerspective(activePerspective.id, group.perspective))
    .flatMap((group) =>
      [...group.children]
        .filter((page) => isFeatureEnabled(page.requiredFeatureFlag))
        .filter((page) => isPermitted(page.permissions))
        .slice(1)
        .map((page) => ({ type: PAGE_TYPE, link: page.path, title: `${group.description} / ${page.description}` })),
    );
};

const normalize = (s: string) => s.toLocaleLowerCase().trim();

const scoreItem = (item: { title: string }, query: string) => {
  const normalizedTitle = normalize(item.title);
  const normalizedQuery = normalize(query);
  if (normalizedTitle === normalizedQuery) {
    return BASE_SCORE;
  }
  if (normalizedTitle.startsWith(query)) {
    return BASE_SCORE - 1;
  }
  if (normalizedTitle.includes(normalizedQuery)) {
    return BASE_SCORE - 2;
  }

  return 0;
};

const useScoreResults = (items: Array<SearchResultItem>, query: string, weight = 1.0) =>
  items.flatMap((item) => {
    const score = scoreItem(item, query);
    if (score === 0) {
      return [];
    }

    return [{ ...item, score: score * weight }];
  });

const useEntityCreatorItems = () => {
  const { isPermitted } = usePermissions();
  const entityCreators = usePluginEntities('entityCreators');

  return entityCreators
    .filter((creator) => (creator.permissions ? isPermitted(creator.permissions) : true))
    .map((creator) => ({ type: PAGE_TYPE, link: creator.path, title: creator.title }));
};

const useConfigurationPages = () => {
  const { isPermitted } = usePermissions();
  const coreSystemConfigurations = usePluginEntities('coreSystemConfigurations');
  const pluginSystemConfigurations = usePluginEntities('systemConfigurations');

  const coreNavItems = coreSystemConfigurations
    .filter(({ permissions }) => isPermitted(permissions))
    .map((page) => ({
      type: PAGE_TYPE,
      link: prefixUrl(`${Routes.SYSTEM.CONFIGURATIONS}/${page.name}`),
      title: `Configurations / ${page.name}`,
    }));

  const pluginNavItems = pluginSystemConfigurations
    // eslint-disable-next-line react-hooks/rules-of-hooks
    .filter(({ useCondition }) => (typeof useCondition === 'function' ? useCondition() : true))
    .map((page) => ({
      type: PAGE_TYPE,
      link: prefixUrl(`${Routes.SYSTEM.configurationsSection('Plugins', page.configType)}`),
      title: `Configurations / ${page.displayName}`,
    }));

  return [...coreNavItems, ...pluginNavItems];
};

const useQuickJumpSearch = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const mainNavItems = useMainNavigationItems();
  const pageNavItems = usePageNavigationItems();
  const creatorItems = useEntityCreatorItems();
  const configurationPageNavItems = useConfigurationPages();
  const { data: entityItems, isLoading } = useEntitySearchResults({ query: searchQuery });

  const scoredNavItems = useScoreResults(
    [...mainNavItems, ...pageNavItems, ...creatorItems, ...configurationPageNavItems],
    searchQuery,
    PAGE_WEIGHT,
  );

  const searchResults = useMemo(
    () =>
      entityItems ? [...entityItems, ...scoredNavItems].sort((result1, result2) => result2.score - result1.score) : [],
    [entityItems, scoredNavItems],
  );

  return {
    isLoading,
    searchQuery,
    searchResults,
    setSearchQuery,
  };
};

export default useQuickJumpSearch;
