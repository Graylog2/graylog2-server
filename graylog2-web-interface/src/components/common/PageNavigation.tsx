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
import { useMemo } from 'react';
import type { PluginNavigation } from 'graylog-web-plugin';

import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';
import sortNavigationItems from 'components/navigation/util/sortNavigationItems';
import usePluginEntities from 'hooks/usePluginEntities';
import mergeNavigationItems from 'components/navigation/util/mergeNavigationItems';
import AppConfig from 'util/AppConfig';
import { DEFAULT_PERSPECTIVE } from 'components/perspectives/contexts/PerspectivesProvider';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';
import NavTabs from 'components/common/NavTabs';

type PageNavItem = {
  description: string;
  path: string;
  permissions?: string | Array<string>;
  exactPathMatch?: boolean;
  useCondition?: () => boolean;
  position?: PluginNavigation['position'];
};

const matchesPerspective = (activePerspective: string, itemPerspective: string) =>
  activePerspective === DEFAULT_PERSPECTIVE ? !itemPerspective : itemPerspective === activePerspective;

const usePageNavigationItems = (page: string, items: Array<PageNavItem>) => {
  const { activePerspective } = useActivePerspective();
  const allPageNavigationItems = usePluginEntities('pageNavigation');

  return useMemo(() => {
    if (items) {
      return items;
    }
    const perspectiveNavItems = allPageNavigationItems.filter((group) =>
      matchesPerspective(activePerspective.id, group.perspective),
    );

    return mergeNavigationItems(perspectiveNavItems).find((item) => item.description === page)?.children ?? [];
  }, [items, allPageNavigationItems, activePerspective.id, page]);
};

// Please provide items by via the plugin system instead of the items prop where possible.
type Props = { page?: string; items?: Array<PageNavItem> };

/**
 * Simple tab navigation to allow navigating to subareas of a page.
 */
const PageNavigation = ({ page = undefined, items: itemsProp = undefined }: Props) => {
  const currentUser = useCurrentUser();
  const items = usePageNavigationItems(page, itemsProp);

  const availableItems = items.filter(
    (item) =>
      (item.requiredFeatureFlag ? AppConfig.isFeatureEnabled(item.requiredFeatureFlag) : true) &&
      (typeof item.useCondition === 'function' ? item.useCondition() : true) &&
      isPermitted(currentUser.permissions, item.permissions) &&
      !!item.path,
  );

  const formatedItems = sortNavigationItems<PageNavItem>(availableItems);

  if (formatedItems.length === 0) {
    return null;
  }

  return <NavTabs items={formatedItems} />;
};

export default PageNavigation;
