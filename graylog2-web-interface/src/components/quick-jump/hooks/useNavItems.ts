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
import { useContext, useMemo } from 'react';
import type { Permission } from 'graylog-web-plugin/plugin';

import usePluginEntities from 'hooks/usePluginEntities';
import { PAGE_TYPE, ACTION_TYPE, LINK_TYPE } from 'components/quick-jump/Constants';
import usePermissions from 'hooks/usePermissions';
import type { QualifiedUrl } from 'routing/Routes';
import Routes, { prefixUrl } from 'routing/Routes';
import AppConfig from 'util/AppConfig';
import type { SearchResultItem } from 'components/quick-jump/Types';
import useCurrentUser from 'hooks/useCurrentUser';
import { ScratchpadContext } from 'contexts/ScratchpadProvider';

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

const useQuickJumpActions = (): SearchResultItem[] => {
  const { isScratchpadVisible } = useContext(ScratchpadContext);

  return [
    {
      type: ACTION_TYPE,
      title: 'Logout current user',
      action: ({ logout }) => {
        logout();
      },
    },
    {
      type: ACTION_TYPE,
      title: 'Toggle Theme',
      action: ({ toggleThemeMode }) => {
        toggleThemeMode();
      },
    },
    {
      type: ACTION_TYPE,
      title: `${isScratchpadVisible ? 'Hide' : 'Show'} Scratchpad`,
      action: ({ toggleScratchpad }) => {
        toggleScratchpad();
      },
    },
  ];
};

const useQuickJumpLinks = () => {
  const { readOnly, id: userId } = useCurrentUser();

  return [
    {
      type: PAGE_TYPE,
      title: `${readOnly ? 'Show' : 'Edit'} profile for current user`,
      link: readOnly ? Routes.SYSTEM.USERS.show(userId) : Routes.SYSTEM.USERS.edit(userId),
    },
    {
      type: PAGE_TYPE,
      title: 'Welcome',
      link: Routes.WELCOME,
    },
  ];
};

const useHelpMenuItems = () => {
  const menuItems = usePluginEntities('helpMenu');
  const { isPermitted } = usePermissions();

  return menuItems
    .filter((item) => isPermitted(item.permissions))
    .map((item) => {
      if ('externalLink' in item) {
        return {
          type: LINK_TYPE,
          externalLink: item.externalLink,
          title: item.description,
        };
      }

      if ('action' in item) {
        return {
          type: ACTION_TYPE,
          title: item.description,
          action: item.action,
        };
      }

      if ('path' in item) {
        return {
          type: PAGE_TYPE,
          title: item.description,
          link: prefixUrl(item.path),
        };
      }

      throw Error('Help menu item must have either external link or action defined');
    });
};

const isFeatureEnabled = (featureFlag?: string) => {
  if (!featureFlag) return true;

  return AppConfig.isFeatureEnabled(featureFlag);
};

type BaseNavigationItem = {
  description: string;
  path: QualifiedUrl<string>;
  permissions?: Permission | Array<Permission>;
  perspective?: string;
};

const useMainNavigationItems = () => {
  const { isPermitted } = usePermissions();
  const navigationItems = usePluginEntities('navigation');

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
    .filter((item) => isPermitted(item.permissions))
    .map((item) => ({ type: PAGE_TYPE, link: item.path, title: item.description }));
};

const usePageNavigationItems = () => {
  const { isPermitted } = usePermissions();
  const pageNavigationItems = usePluginEntities('pageNavigation');

  return pageNavigationItems.flatMap((group) =>
    [...group.children]
      .filter((page) => isFeatureEnabled(page.requiredFeatureFlag))
      .filter((page) => isPermitted(page.permissions))
      .slice(1)
      .map((page) => ({ type: PAGE_TYPE, link: page.path, title: `${group.description} / ${page.description}` })),
  );
};

const useNavItems = () => {
  const mainNavItems = useMainNavigationItems();
  const pageNavItems = usePageNavigationItems();
  const creatorItems = useEntityCreatorItems();
  const configurationPageNavItems = useConfigurationPages();
  const quickJumpActions = useQuickJumpActions();
  const quickJumpLinks = useQuickJumpLinks();
  const helpMenuItems = useHelpMenuItems();

  return useMemo(
    () => [
      ...mainNavItems,
      ...pageNavItems,
      ...creatorItems,
      ...configurationPageNavItems,
      ...quickJumpActions,
      ...quickJumpLinks,
      ...helpMenuItems,
    ],
    [
      configurationPageNavItems,
      creatorItems,
      helpMenuItems,
      mainNavItems,
      pageNavItems,
      quickJumpActions,
      quickJumpLinks,
    ],
  );
};

export default useNavItems;
