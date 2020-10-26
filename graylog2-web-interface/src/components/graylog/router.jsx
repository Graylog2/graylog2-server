// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import type { LocationShape } from 'react-router';

import withLocation, { type Location } from 'routing/withLocation';
import history from 'util/History';

export type HistoryElement = LocationShape;

const _setActiveClassName = (location, to, currentClassName, displayName) => {
  const props = {};
  const isActive = to === location.pathname;

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
  location: Location,
};

const LinkContainer = withLocation(({ children, onClick, to, location, ...rest }: Props) => {
  const { props: { onClick: childrenOnClick, className }, type: { displayName } } = React.Children.only(children);
  const _onClick = useCallback(() => {
    if (childrenOnClick) {
      childrenOnClick();
    }

    if (onClick) {
      onClick();
    }

    history.push(to);
  }, [childrenOnClick, onClick, to]);
  const childrenClassName = _setActiveClassName(location, to, className, displayName);

  return React.cloneElement(React.Children.only(children), { ...rest, ...childrenClassName, onClick: _onClick });
});

export {
  Link,
  LinkContainer,
};
