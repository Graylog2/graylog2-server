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
import { SyntheticEvent } from 'react';
import PropTypes from 'prop-types';
import { Overlay } from 'react-overlays';

import { DropdownMenu, MenuItem } from 'components/graylog';

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
};

type ActionDropdownState = {
  show: boolean,
};

type FilterPropsProps = {
  children: React.ReactElement,
  style?: { [key: string]: any },
};

type ActionDropdownProps = {
  children: React.ReactElement,
  container?: HTMLElement,
  element: React.ReactNode,
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
  bsRole: undefined,
};

const FilterProps = ({ children, style }: FilterPropsProps) => {
  const mappedChildren = React.Children.map(
    children,
    (child) => React.cloneElement(child, { style: { ...style, ...child.props.style } }),
  );

  return <>{mappedChildren}</>;
};

class ActionDropdown extends React.Component<ActionDropdownProps, ActionDropdownState> {
  target: HTMLElement | undefined | null;

  static defaultProps = {
    container: undefined,
  };

  static propTypes = {
    children: PropTypes.node.isRequired,
    container: PropTypes.oneOfType([PropTypes.node, PropTypes.func]),
    element: PropTypes.node.isRequired,
  };

  constructor(props: ActionDropdownProps) {
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

  closeOnChildSelect = (child: React.ReactElement, updateDepth: number) => {
    if (child.props?.onSelect) {
      return {
        onSelect: (eventKey: string | null | undefined, event: SyntheticEvent<HTMLButtonElement>) => {
          child.props.onSelect();
          this._onToggle(event);
        },
      };
    }

    if (child.props?.children) {
      return {
        children: this.closeOnChildrenSelect(child.props.children, updateDepth + 1),
      };
    }

    return {};
  };

  closeOnChildrenSelect = (children: React.ReactNode, updateDepth: number) => {
    const maxChildDepth = 2;

    if (updateDepth > maxChildDepth) {
      return children;
    }

    return React.Children.map(
      children,
      (child: React.ReactElement) => (child?.props ? React.cloneElement(child, {
        ...child.props,
        ...this.closeOnChildSelect(child, updateDepth + 1),
      }) : child),
    );
  }

  render() {
    const { children, container, element } = this.props;
    const { show } = this.state;
    const mappedChildren = this.closeOnChildrenSelect(children, 0);

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

export default ActionDropdown;
