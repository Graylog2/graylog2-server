// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import { MenuItem } from 'components/graylog';
import { Overlay } from 'react-overlays';
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

const StyledDropdownMenu: StyledComponent<ActionDropdownState, ThemeInterface, HTMLUListElement> = styled.ul(({ show, theme }) => `
  display: ${show ? 'block' : 'none'};
  min-width: max-content;
  color: ${theme.colors.gray[40]};
`);

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
            <StyledDropdownMenu show={show} className="dropdown-menu">
              <MenuItem header>Actions</MenuItem>
              {mappedChildren}
            </StyledDropdownMenu>
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
