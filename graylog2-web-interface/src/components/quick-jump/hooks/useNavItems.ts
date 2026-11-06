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
import { useTranslation } from 'react-i18next';
import type { Permission } from 'graylog-web-plugin/plugin';

import useNavigationTitle from 'components/navigation/useNavigationTitle';
import usePluginEntities from 'hooks/usePluginEntities';
import { PAGE_TYPE, ACTION_TYPE, LINK_TYPE } from 'components/quick-jump/Constants';
import usePermissions from 'hooks/usePermissions';
import type { QualifiedUrl } from 'routing/Routes';
import Routes, { prefixUrl } from 'routing/Routes';
import AppConfig from 'util/AppConfig';
import type { SearchResultItem } from 'components/quick-jump/Types';
import useCurrentUser from 'hooks/useCurrentUser';
import { ScratchpadContext } from 'contexts/ScratchpadProvider';
import { isTelemetryDebugEnabled, setTelemetryDebugEnabled } from 'logic/telemetry/debug/TelemetryDebugStore';

const isFeatureEnabled = (featureFlag?: string) => {
  if (!featureFlag) return true;

  return AppConfig.isFeatureEnabled(featureFlag);
};

const useEntityCreatorItems = () => {
  const { isPermitted } = usePermissions();
  const entityCreators = usePluginEntities('entityCreators');

  return entityCreators
    .filter((creator) => (creator.permissions ? isPermitted(creator.permissions) : true))
    .filter((creator) => isFeatureEnabled(creator.requiredFeatureFlag))
    .map((creator) => ({ type: PAGE_TYPE, link: creator.path, title: creator.title }));
};

const useConfigurationPages = () => {
  const { t } = useTranslation('common');
  const { isPermitted } = usePermissions();
  const coreSystemConfigurations = usePluginEntities('coreSystemConfigurations');
  const pluginSystemConfigurations = usePluginEntities('systemConfigurations');

  const coreNavItems = coreSystemConfigurations
    .filter(({ permissions }) => isPermitted(permissions))
    .map((page) => ({
      type: PAGE_TYPE,
      link: prefixUrl(`${Routes.SYSTEM.CONFIGURATIONS}/${page.name}`),
      title: t('quick_jump.configuration_page', { name: page.displayName ?? page.name }),
    }));

  const pluginNavItems = pluginSystemConfigurations
    // eslint-disable-next-line react-hooks/rules-of-hooks
    .filter(({ useCondition }) => useCondition?.() ?? true)
    .map((page) => ({
      type: PAGE_TYPE,
      link: prefixUrl(`${Routes.SYSTEM.configurationsSection('Plugins', page.configType)}`),
      title: t('quick_jump.configuration_page', { name: page.displayName }),
    }));

  return [...coreNavItems, ...pluginNavItems];
};

const useQuickJumpActions = (): SearchResultItem[] => {
  const { t } = useTranslation('common');
  const { isScratchpadVisible } = useContext(ScratchpadContext);

  return [
    {
      type: ACTION_TYPE,
      title: t('quick_jump.actions.logout'),
      action: ({ logout }) => {
        logout();
      },
    },
    {
      type: ACTION_TYPE,
      title: t('quick_jump.actions.toggle_theme'),
      action: ({ toggleThemeMode }) => {
        toggleThemeMode();
      },
    },
    {
      type: ACTION_TYPE,
      title: isScratchpadVisible ? t('quick_jump.actions.hide_scratchpad') : t('quick_jump.actions.show_scratchpad'),
      action: ({ toggleScratchpad }) => {
        toggleScratchpad();
      },
    },
    // The overlay itself works in any build (localStorage flag); only this discoverable entry
    // point is limited to development mode.
    ...(AppConfig.gl2DevMode()
      ? [
          {
            type: ACTION_TYPE,
            title: `${isTelemetryDebugEnabled() ? 'Disable' : 'Enable'} Telemetry Debug Overlay`,
            action: () => {
              setTelemetryDebugEnabled(!isTelemetryDebugEnabled());
            },
          },
        ]
      : []),
  ];
};

const useQuickJumpLinks = () => {
  const { t } = useTranslation('common');
  const { readOnly, id: userId } = useCurrentUser();

  return [
    {
      type: PAGE_TYPE,
      title: readOnly ? t('quick_jump.links.show_profile') : t('quick_jump.links.edit_profile'),
      link: readOnly ? Routes.SYSTEM.USERS.show(userId) : Routes.SYSTEM.USERS.edit(userId),
    },
    {
      type: PAGE_TYPE,
      title: t('quick_jump.links.welcome'),
      link: Routes.WELCOME,
    },
  ];
};

const useHelpMenuItems = () => {
  const menuItems = usePluginEntities('helpMenu');
  const navigationTitle = useNavigationTitle();
  const { isPermitted } = usePermissions();

  return menuItems
    .filter((item) => isPermitted(item.permissions))
    .map((item) => {
      if ('externalLink' in item) {
        return {
          type: LINK_TYPE,
          externalLink: item.externalLink,
          title: navigationTitle(item),
        };
      }

      if ('action' in item) {
        return {
          type: ACTION_TYPE,
          title: navigationTitle(item),
          action: item.action,
        };
      }

      if ('path' in item) {
        return {
          type: PAGE_TYPE,
          title: navigationTitle(item),
          link: prefixUrl(item.path),
        };
      }

      throw Error('Help menu item must have either external link or action defined');
    });
};

type BaseNavigationItem = {
  title: string;
  path: QualifiedUrl<string>;
  permissions?: Permission | Array<Permission>;
};

const useMainNavigationItems = () => {
  const { isPermitted } = usePermissions();
  const navigationItems = usePluginEntities('navigation');
  const navigationTitle = useNavigationTitle();

  const allNavigationItems = navigationItems.flatMap((item) => {
    // Conditions are hooks, so they are evaluated for every item and child before the early return.
    const itemVisible =
      (item.useCondition?.() ?? true) && isFeatureEnabled(item.requiredFeatureFlag) && isPermitted(item.permissions);
    const visibleChildren =
      'children' in item
        ? item.children.filter(
            (child) => (child.useCondition?.() ?? true) && isFeatureEnabled(child.requiredFeatureFlag),
          )
        : [];

    // A dropdown's feature flag, condition and permissions also apply to its children.
    if (!itemVisible) {
      return [];
    }

    if (!('children' in item)) {
      return [{ ...item, title: navigationTitle(item) }];
    }

    return visibleChildren.map<BaseNavigationItem>((child) => ({
      ...child,
      title: `${navigationTitle(item)} / ${navigationTitle(child)}`,
    }));
  });

  return allNavigationItems
    .filter((item) => isPermitted(item.permissions))
    .map((item) => ({ type: PAGE_TYPE, link: item.path, title: item.title }));
};

const usePageNavigationItems = () => {
  const { isPermitted } = usePermissions();
  const pageNavigationItems = usePluginEntities('pageNavigation');
  const navigationTitle = useNavigationTitle();

  return pageNavigationItems.flatMap((group) =>
    [...group.children]
      .filter(
        ({ useCondition, requiredFeatureFlag }) =>
          // eslint-disable-next-line react-hooks/rules-of-hooks
          (useCondition?.() ?? true) && isFeatureEnabled(requiredFeatureFlag),
      )
      .filter((page) => isPermitted(page.permissions))
      .slice(1)
      .map((page) => ({
        type: PAGE_TYPE,
        link: page.path,
        title: `${navigationTitle(group)} / ${navigationTitle(page)}`,
      })),
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
