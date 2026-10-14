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
import { useTranslation } from 'react-i18next';
import type { PluginNavigation } from 'graylog-web-plugin';
import styled, { css } from 'styled-components';

import useNavigationTitle from 'components/navigation/useNavigationTitle';
import { Menu } from 'components/bootstrap';
import { Icon } from 'components/common';
import { hoverIndicatorStyles } from 'components/common/NavItemStateIndicator';
import usePermissions from 'hooks/usePermissions';
import NavigationLink from 'components/navigation/NavigationLink';
import shouldRenderNavigationItem from 'components/navigation/util/shouldRenderNavigationItem';
import { NAV_ITEM_HEIGHT } from 'theme/constants';

const BurgerToggle = styled.button(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    background: transparent;
    border: 0;
    padding: 0 15px;
    min-height: ${NAV_ITEM_HEIGHT};
    color: ${theme.colors.text.primary};

    &:hover,
    &:focus-visible {
      ${hoverIndicatorStyles(theme)}
    }
  `,
);

type Props = {
  navigationItems: Array<PluginNavigation>;
};

const CollapsedMainNavbar = ({ navigationItems }: Props) => {
  const { isPermitted } = usePermissions();
  const navigationTitle = useNavigationTitle();
  const { t } = useTranslation('navigation');

  const accessibleItems = navigationItems.filter(({ requiredFeatureFlag, permissions }) =>
    shouldRenderNavigationItem(requiredFeatureFlag, permissions, isPermitted),
  );

  return (
    <Menu>
      <Menu.Target>
        <BurgerToggle type="button" aria-label={t('shell.toggle_navigation')}>
          <Icon name="menu" size="lg" />
        </BurgerToggle>
      </Menu.Target>
      <Menu.Dropdown>
        {accessibleItems.map(({ description, titleKey, path, children, BadgeComponent }) => {
          if (!children) {
            return (
              <NavigationLink
                key={description}
                description={navigationTitle({ description, titleKey })}
                path={path}
                Badge={BadgeComponent}
              />
            );
          }

          const accessibleChildren = children.filter(
            ({ requiredFeatureFlag, permissions, useCondition }) =>
              // eslint-disable-next-line react-hooks/rules-of-hooks
              (useCondition?.() ?? true) && shouldRenderNavigationItem(requiredFeatureFlag, permissions, isPermitted),
          );

          if (!accessibleChildren.length) {
            return null;
          }

          return (
            <Menu.Sub key={description}>
              <Menu.Sub.Target>
                <Menu.Sub.Item>{navigationTitle({ description, titleKey })}</Menu.Sub.Item>
              </Menu.Sub.Target>
              <Menu.Sub.Dropdown>
                {accessibleChildren.map((child) => (
                  <NavigationLink
                    key={child.description}
                    description={navigationTitle(child)}
                    path={child.path}
                    Badge={child.BadgeComponent}
                  />
                ))}
              </Menu.Sub.Dropdown>
            </Menu.Sub>
          );
        })}
      </Menu.Dropdown>
    </Menu>
  );
};

export default CollapsedMainNavbar;
