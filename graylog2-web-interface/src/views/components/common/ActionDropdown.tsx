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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Overlay } from 'react-overlays';

import { DropdownMenu, MenuItem } from 'components/graylog';

import StopPropagation from './StopPropagation';

/**
 * This implements a custom toggle for a dropdown menu.
 * See: "Custom Dropdown Components" in react-bootstrap documentation.
 */

type ActionToggleProps = {
  children: React.Node,
  onClick: (SyntheticInputEvent<HTMLButtonElement>) => void,
};

type ActionDropdownState = {
  show: boolean,
};

type FilterPropsProps = {
  children: React.Node,
  style?: { [string]: any },
};

type ActionDropdownProps = {
  children: React.Node,
  container?: HTMLElement,
  element: React.Node,
};

const ActionToggle = ({ children, onClick }: ActionToggleProps) => {
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
    <span onClick={handleClick} onKeyDown={handleKeyDown} role="presentation">
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
};

const FilterProps = ({ children, style }: FilterPropsProps) => React.Children.map(
  children,
  (child) => React.cloneElement(child, { style: { ...style, ...child.props.style } }),
);

class ActionDropdown extends React.Component<ActionDropdownProps, ActionDropdownState> {
  target: ?HTMLElement;

  static defaultProps = {
    container: undefined,
  };

  constructor(props: ActionDropdownProps) {
    super(props);

    this.state = {
      show: false,
    };
  }

  _onToggle = (e: SyntheticInputEvent<HTMLButtonElement>) => {
    e.preventDefault();
    e.stopPropagation();
    this.setState(({ show }) => ({ show: !show }));
  };

  render() {
    const { children, container, element } = this.props;
    const { show } = this.state;

    const mappedChildren = React.Children.map(
      children,
      (child) => child && React.cloneElement(child, {
        ...child.props,
        ...(child.props.onSelect ? {
          onSelect: (eventKey, event) => {
            child.props.onSelect();
            this._onToggle(event);
          },
        } : {}),
      }),
    );

    return (
      <StopPropagation>
        <ActionToggle bsRole="toggle" onClick={this._onToggle}>
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
            <DropdownMenu show={show}>
              <MenuItem header>Actions</MenuItem>
              {mappedChildren}
            </DropdownMenu>
          </FilterProps>
        </Overlay>
      </StopPropagation>
    );
  }
}

ActionDropdown.propTypes = {
  children: PropTypes.node.isRequired,
  container: PropTypes.oneOfType([PropTypes.node, PropTypes.func]),
  element: PropTypes.node.isRequired,
};

export default ActionDropdown;
