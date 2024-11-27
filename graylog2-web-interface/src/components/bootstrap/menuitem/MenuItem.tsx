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
import { useCallback } from 'react';
import styled from 'styled-components';
import { Link } from 'react-router-dom';

import Icon from 'components/common/Icon';

import Menu from '../Menu';

const IconWrapper = styled.div`
  display: inline-flex;
  min-width: 20px;
  margin-right: 5px;
  justify-content: center;
  align-items: center;
`;

type Callback<T> = T extends undefined ? () => void : (eventKey: T) => void

type Props<T = undefined> = React.PropsWithChildren<{
  active?: boolean,
  className?: string,
  component?: 'a',
  'data-tab-id'?: string,
  disabled?: boolean,
  divider?: boolean,
  eventKey?: T,
  header?: boolean,
  href?: string,
  icon?: React.ComponentProps<typeof Icon>['name'],
  id?: string,
  onClick?: Callback<T>,
  onSelect?: Callback<T>,
  rel?: 'noopener noreferrer',
  target?: '_blank',
  title?: string,
  closeMenuOnClick?: boolean,
}>;

const CustomMenuItem = <T, >({ children, className, disabled = false, divider = false, eventKey, header = false, href, icon, id, onClick, onSelect, rel, target, title, 'data-tab-id': dataTabId, component, closeMenuOnClick }: Props<T>) => {
  const callback = onClick ?? onSelect;
  const _onClick = useCallback(() => callback?.(eventKey), [callback, eventKey]);

  if (divider) {
    return <Menu.Divider role="separator" className={className} id={id} />;
  }

  if (header) {
    return <Menu.Label role="heading" className={className} id={id}>{children}</Menu.Label>;
  }

  const sharedProps = {
    className,
    'data-tab-id': dataTabId,
    disabled,
    icon: icon ? <IconWrapper><Icon name={icon} /></IconWrapper> : null,
    id,
    onClick: _onClick,
    title,
    closeMenuOnClick,
  };

  if (href) {
    return (
      <Menu.Item component={Link} to={href} rel={rel} target={target} {...sharedProps}>
        {children}
      </Menu.Item>
    );
  }

  return (
    <Menu.Item component={component} {...sharedProps}>
      {children}
    </Menu.Item>
  );
};

/** @component */
export default CustomMenuItem;
