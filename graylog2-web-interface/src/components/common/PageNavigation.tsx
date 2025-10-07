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
import styled, { css } from 'styled-components';
import { useMemo } from 'react';
import type { PluginNavigation } from 'graylog-web-plugin';

import { Button, ButtonToolbar } from 'components/bootstrap';
import { LinkContainer } from 'components/common/router';
import NavItemStateIndicator, {
  hoverIndicatorStyles,
  activeIndicatorStyles,
} from 'components/common/NavItemStateIndicator';
import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';
import sortNavigationItems from 'components/navigation/util/sortNavigationItems';
import usePluginEntities from 'hooks/usePluginEntities';
import mergeNavigationItems from 'components/navigation/util/mergeNavigationItems';

const Container = styled(ButtonToolbar)`
  margin-bottom: 10px;
`;

const StyledButton = styled(Button)(
  ({ theme }) => css`
  font-family: ${theme.fonts.family.navigation};
  font-size: ${theme.fonts.size.navigation};
  color: ${theme.colors.text.primary};
  
  &:hover,
  &:focus {
    background: inherit;
    text-decoration: none;
  }

  &:hover {
    color: inherit;
    ${hoverIndicatorStyles(theme)}
  }

  &.active {
    color: ${theme.colors.text.primary};

    ${activeIndicatorStyles(theme)}

    &:hover,
    &:focus-visible {
      ${activeIndicatorStyles(theme)}
    }
`,
);

StyledButton.displayName = 'Button';

const usePageNavigationItems = (page: string, itemsProp: Array<PageNavItem>) => {
  const allPageNavigationItems = usePluginEntities('pageNavigation');

  return useMemo(() => {
    if (itemsProp) {
      return itemsProp;
    }

    return mergeNavigationItems(allPageNavigationItems).find((item) => item.description === page)?.children ?? [];
  }, [allPageNavigationItems, itemsProp, page]);
};

type PageNavItem = {
  description: string;
  path: string;
  permissions?: string | Array<string>;
  exactPathMatch?: boolean;
  useIsValidLicense?: () => boolean;
  position?: PluginNavigation['position'];
};

type Props = {
  // key of the group in the plugin system
  page?: string;
  items?: Array<PageNavItem>;
};

/**
 * Simple tab navigation to allow navigating to subareas of a page.
 */
const PageNavigation = ({ page = undefined, items: itemsProp = undefined }: Props) => {
  const currentUser = useCurrentUser();

  const items = usePageNavigationItems(page, itemsProp);
  const formatedItems = useMemo(() => {
    const availableItems = items.filter(
      (item) =>
        (typeof item.useIsValidLicense === 'function' ? item.useIsValidLicense() : true) &&
        isPermitted(currentUser.permissions, item.permissions) &&
        !!item.path,
    );

    return sortNavigationItems<PageNavItem>(availableItems);
  }, [currentUser.permissions, items]);

  return (
    <Container>
      {formatedItems.map(({ path, description, exactPathMatch }) => (
        <LinkContainer to={path} relativeActive={!exactPathMatch} key={path}>
          <StyledButton bsStyle="transparent">
            <NavItemStateIndicator>{description}</NavItemStateIndicator>
          </StyledButton>
        </LinkContainer>
      ))}
    </Container>
  );
};

export default PageNavigation;
