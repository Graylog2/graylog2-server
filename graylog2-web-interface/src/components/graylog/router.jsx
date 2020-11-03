// @flow strict
import * as React from 'react';
import { useCallback, useMemo } from 'react';
import { Link, useLocation } from 'react-router-dom';
import type { LocationShape } from 'react-router';

import history from 'util/History';

export type HistoryElement = LocationShape;

const _targetPathname = (to) => {
  const target = typeof to?.pathname === 'string' ? to.pathname : to;

  return String(target).split(/[?#]/)[0];
};

const _setActiveClassName = (pathname, to, currentClassName, displayName) => {
  const targetPathname = _targetPathname(to);
  const isActive = targetPathname === pathname;
  const isButton = displayName === 'Button';

  return isButton && isActive
    ? `active ${currentClassName ?? ''}`
    : currentClassName;
};

type Props = {
  children: React.Node,
  onClick?: () => mixed,
  to: string | HistoryElement,
};

const isLeftClickEvent = (e) => (e.button === 0);

const isModifiedEvent = (e) => !!(e.metaKey || e.altKey || e.ctrlKey || e.shiftKey);

const LinkContainer = ({ children, onClick, to, ...rest }: Props) => {
  const { pathname } = useLocation();
  const { props: { onClick: childrenOnClick, className }, type: { displayName } } = React.Children.only(children);
  const childrenClassName = useMemo(() => _setActiveClassName(pathname, to, className, displayName), [pathname, to, className, displayName]);
  const _onClick = useCallback((e) => {
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

export {
  Link,
  LinkContainer,
};
