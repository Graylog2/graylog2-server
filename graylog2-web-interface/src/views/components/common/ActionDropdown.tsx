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
import type { SyntheticEvent } from 'react';
import PropTypes from 'prop-types';
import { Overlay } from 'react-overlays';

import { DropdownMenu } from 'components/common';
import { MenuItem } from 'components/bootstrap';

import StopPropagation from './StopPropagation';

/**
 * This implements a custom toggle for a dropdown menu.
 * See: "Custom Dropdown Components" in react-bootstrap documentation.
 */

type ActionToggleProps = {
  children: React.ReactElement | Array<React.ReactElement>,
  onClick: (event: SyntheticEvent) => void,
  // eslint-disable-next-line react/no-unused-prop-types
  bsRole?: string,
  title: string,
};

type ActionDropdownState = {
  show: boolean,
};

type FilterPropsProps = {
  children: React.ReactElement,
  style?: { [key: string]: any },
};

const ActionToggle = ({ children, onClick, title }: ActionToggleProps) => {
  const handleClick = (e) => {
    e.preventDefault();
    e.stopPropagation();

    onClick(e);
  };

  const handleKeyDown = (e) => {
    if (e.keyCode === 32) {
      e.preventDefault();
      e.stopPropagation();

      onClick(e);
    }
  };

  return (
    <span onClick={handleClick} onKeyDown={handleKeyDown} role="presentation" title={title}>
      {children}
    </span>
  );
};

ActionToggle.propTypes = {
  children: PropTypes.node.isRequired,
  onClick: PropTypes.func,
};

ActionToggle.defaultProps = {
  onClick: () => {},
  bsRole: undefined,
};

const FilterProps = ({ children, style }: FilterPropsProps) => (
  <>
    {React.Children.map(
      children,
      (child) => React.cloneElement(child, { style: { ...style, ...child.props.style } }),
    )}
  </>
);

FilterProps.defaultProps = {
  style: {},
};

type Props = {
  children: React.ReactNode,
  container?: HTMLElement,
  element: React.ReactNode,
  title?: string,
};

class ActionDropdown extends React.Component<Props, ActionDropdownState> {
  target: HTMLElement | undefined | null;

  static defaultProps = {
    container: undefined,
    title: 'Actions',
  };

  static propTypes = {
    children: PropTypes.node.isRequired,
    container: PropTypes.oneOfType([PropTypes.node, PropTypes.func]),
    element: PropTypes.node.isRequired,
  };

  constructor(props: Props) {
    super(props);

    this.state = {
      show: false,
    };
  }

  _onToggle = (e: SyntheticEvent) => {
    e.preventDefault();
    e.stopPropagation();
    this.setState(({ show }) => ({ show: !show }));
  };

  render() {
    const { children, container, element, title } = this.props;
    const { show } = this.state;

    return (
      <StopPropagation>
        <ActionToggle bsRole="toggle" onClick={this._onToggle} title={title}>
          <span ref={(elem) => { this.target = elem; }}>{element}</span>
        </ActionToggle>
        <Overlay show={show}
                 container={container}
                 placement="bottom"
                 shouldUpdatePosition
                 rootClose
                 onHide={this._onToggle}
                 target={() => this.target}>
          <FilterProps>
            <DropdownMenu show={show} onMenuItemSelect={this._onToggle}>
              <MenuItem header>Actions</MenuItem>
              {children}
            </DropdownMenu>
          </FilterProps>
        </Overlay>
      </StopPropagation>
    );
  }
}

export default ActionDropdown;
