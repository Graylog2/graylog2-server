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
import styled from 'styled-components';

import { Nav } from 'components/bootstrap';
import NavigationItem from 'components/navigation/NavigationItem';
import CollapsedMainNavbar from 'components/navigation/CollapsedMainNavbar';
import useNavigationItems from 'components/navigation/useNavigationItems';

// Must not shrink: its measured width tells `useNavigationCollapse` how much room the menu wants,
// which a shrunk menu would understate.
const ExpandedNav = styled(Nav)`
  flex: 0 0 auto;
  align-items: stretch;

  > li {
    display: inline-flex;
    align-items: stretch;
  }

  > li > a {
    display: flex;
    align-items: center;
  }
`;

type Props = {
  pathname: string;
  collapsed?: boolean;
  menuRef?: React.Ref<HTMLUListElement>;
};

const MainNavbar = ({ pathname, collapsed = false, menuRef = undefined }: Props) => {
  const navigationItems = useNavigationItems();

  if (collapsed) {
    return <CollapsedMainNavbar navigationItems={navigationItems} />;
  }

  return (
    <ExpandedNav ref={menuRef}>
      {navigationItems.map((navigationItem) => (
        <NavigationItem navigationItem={navigationItem} pathname={pathname} key={navigationItem.description} />
      ))}
    </ExpandedNav>
  );
};

export default MainNavbar;
