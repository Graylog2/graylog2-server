// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { Link } from 'react-router-dom';

import history from 'util/History';

type Props = {
  children: React.Node,
  onClick?: () => mixed,
  to: string,
};

const LinkContainer = ({ children, onClick, to, ...rest }: Props) => {
  const { props: { onClick: childrenOnClick } } = React.Children.only(children);
  const _onClick = useCallback(() => {
    if (childrenOnClick) {
      childrenOnClick();
    }

    if (onClick) {
      onClick();
    }

    history.push(to);
  }, [childrenOnClick, onClick, to]);

  return React.cloneElement(React.Children.only(children), { ...rest, onClick: _onClick });
};

export {
  Link,
  LinkContainer,
};
