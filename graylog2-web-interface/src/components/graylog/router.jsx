// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { Link, useLocation } from 'react-router-dom';
import type { LocationShape } from 'react-router';

import history from 'util/History';

export type HistoryElement = LocationShape;

const _setActiveClassName = (pathname, to, currentClassName, displayName) => {
  const props = {};
  const isActive = to === pathname;

  if (displayName === 'Button' && isActive) {
    let className = 'active';

    if (currentClassName) {
      className = `${className} ${currentClassName}`;
    }

    props.className = className;
  }

  return props;
};

type Props = {
  children: React.Node,
  onClick?: () => mixed,
  to: string | HistoryElement,
};

const LinkContainer = ({ children, onClick, to, ...rest }: Props) => {
  const { pathname } = useLocation();
  const { props: { onClick: childrenOnClick, className }, type: { displayName } } = React.Children.only(children);
  const childrenClassName = _setActiveClassName(pathname, to, className, displayName);
  const _onClick = useCallback(() => {
    if (childrenOnClick) {
      childrenOnClick();
    }

    if (onClick) {
      onClick();
    }

    history.push(to);
  }, [childrenOnClick, onClick, to]);

  return React.cloneElement(React.Children.only(children), { ...rest, ...childrenClassName, onClick: _onClick });
};

export {
  Link,
  LinkContainer,
};
