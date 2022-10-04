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
import { useCallback, useMemo } from 'react';
import { Link, useLocation } from 'react-router-dom';
import type { Location } from 'history';

import history from 'util/History';

export type HistoryElement = Location;

// list of children which are being used for navigation and should receive the `active` class.
const NAV_CHILDREN = ['Button', 'NavItem'];

const _targetPathname = (to: string) => String(to).split(/[?#]/)[0];

const _setActiveClassName = (pathname: string, to: string, currentClassName: string, displayName: string, relativeActive: boolean) => {
  const targetPathname = _targetPathname(to);
  const isActive = relativeActive ? pathname.startsWith(targetPathname) : targetPathname === pathname;
  const isNavComponent = NAV_CHILDREN.includes(displayName);

  return isNavComponent && isActive
    ? `active ${currentClassName ?? ''}`
    : currentClassName;
};

type ChildrenProps = {
  onClick: (e?: any) => void,
  className: string,
  href: string,
};
type Props = {
  children: React.ReactElement<ChildrenProps, React.ComponentType>,
  onClick?: () => unknown,
  disabled?: boolean,
  to: string | { pathname: string },
  // if set the child component will receive the active class
  // when the part of the URL path matches the `to` prop.
  relativeActive?: boolean,
};

const isLeftClickEvent = (e: React.MouseEvent) => (e.button === 0);

const isModifiedEvent = (e: React.MouseEvent) => !!(e.metaKey || e.altKey || e.ctrlKey || e.shiftKey);

const LinkContainer = ({ children, onClick, to: toProp, relativeActive, ...rest }: Props) => {
  const { pathname } = useLocation();
  const { props: { onClick: childrenOnClick, className }, type: { displayName } } = React.Children.only(children);
  const to = (typeof toProp === 'object' && 'pathname' in toProp) ? toProp.pathname : toProp;
  const childrenClassName = useMemo(() => _setActiveClassName(pathname, to, className, displayName, relativeActive),
    [pathname, to, className, displayName, relativeActive],
  );
  const _onClick = useCallback((e: React.MouseEvent) => {
    if (!isLeftClickEvent(e) || isModifiedEvent(e)) {
      return;
    }

    e.preventDefault();
    e.stopPropagation();

    if (childrenOnClick) {
      childrenOnClick();
    }

    if (onClick) {
      onClick();
    }

    history.push(to);
  }, [childrenOnClick, onClick, to]);

  return React.cloneElement(React.Children.only(children), { ...rest, className: childrenClassName, onClick: _onClick, href: to });
};

LinkContainer.defaultProps = {
  relativeActive: false,
};

export {
  Link,
  LinkContainer,
};
