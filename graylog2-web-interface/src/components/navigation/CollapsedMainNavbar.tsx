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
import type { PluginNavigation } from 'graylog-web-plugin';
import styled, { css } from 'styled-components';

import { Menu } from 'components/bootstrap';
import { Icon } from 'components/common';
import { hoverIndicatorStyles } from 'components/common/NavItemStateIndicator';
import useCurrentUser from 'hooks/useCurrentUser';
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

/**
 * The navigation menu for widths where it does not fit into the navigation bar. Renders the same
 * items as the expanded menu, but as a single dropdown behind a burger toggle, with items which
 * have children becoming nested submenus.
 */
const CollapsedMainNavbar = ({ navigationItems }: Props) => {
  const { permissions } = useCurrentUser();

  const accessibleItems = navigationItems.filter(({ requiredFeatureFlag, permissions: itemPermissions }) =>
    shouldRenderNavigationItem(requiredFeatureFlag, itemPermissions, permissions),
  );

  return (
    <Menu>
      <Menu.Target>
        <BurgerToggle type="button" aria-label="Toggle navigation">
          <Icon name="menu" size="lg" />
        </BurgerToggle>
      </Menu.Target>
      <Menu.Dropdown>
        {accessibleItems.map(({ description, path, children, BadgeComponent }) => {
          if (!children) {
            return <NavigationLink key={description} description={description} path={path} Badge={BadgeComponent} />;
          }

          const accessibleChildren = children.filter(({ requiredFeatureFlag, permissions: childPermissions }) =>
            shouldRenderNavigationItem(requiredFeatureFlag, childPermissions, permissions),
          );

          if (!accessibleChildren.length) {
            return null;
          }

          return (
            <Menu.Sub key={description}>
              <Menu.Sub.Target>
                <Menu.Sub.Item>{description}</Menu.Sub.Item>
              </Menu.Sub.Target>
              <Menu.Sub.Dropdown>
                {accessibleChildren.map((child) => (
                  <NavigationLink
                    key={child.description}
                    description={child.description}
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
