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
import React, { useState, useRef } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Overlay, Transition } from 'react-overlays';

import { DropdownMenu } from 'components/common/index';

type Placement = 'top' | 'right' | 'bottom' | 'left';

const ToggleDropdown = styled.span`
  cursor: pointer;

  .caret {
    visibility: hidden;
  }

  &:hover .caret {
    visibility: visible;
  }
`;

const oppositePlacement = {
  left: 'right',
  right: 'left',
};

type _FilterProps = {
  children: React.ReactElement,
  style?: CSSStyleDeclaration
};

const FilterProps = ({ children, style }: _FilterProps) => (
  <>{React.Children.map(children,
    (child) => React.cloneElement(child, { style: { ...style, ...child.props.style } }))}
  </>
);

FilterProps.defaultProps = {
  style: {},
};

type Props = {
  children: React.ReactNode,
  closeOnSelect?: boolean,
  dropdownMinWidth?: number,
  dropdownZIndex?: number,
  menuContainer?: HTMLElement,
  onToggle: () => void,
  placement?: Placement,
  renderToggle?: (payload: { onToggle: () => void, toggleTarget: React.Ref<HTMLButtonElement> }) => React.ReactNode,
  show: boolean,
  toggleChild?: React.ReactNode,
}

const OverlayDropdown = ({
  children,
  closeOnSelect,
  dropdownMinWidth,
  dropdownZIndex,
  menuContainer,
  onToggle,
  placement,
  renderToggle,
  show,
  toggleChild,
}: Props) => {
  const [currentPlacement, setCurrentPlacement] = useState<Placement>(placement);
  const toggleTarget = useRef<HTMLButtonElement>();

  const handleOverlayEntering = (dropdownElem) => {
    const dropdownOffsetLeft = dropdownElem.offsetLeft;
    const dropdownWidth = dropdownElem.offsetWidth;
    const overflowRight = dropdownOffsetLeft + dropdownWidth >= document.body.clientWidth;
    const overflowLeft = dropdownOffsetLeft < 0;
    const trimmedDropdown = (overflowLeft && currentPlacement === 'left') || (overflowRight && currentPlacement === 'right');

    if (trimmedDropdown) {
      setCurrentPlacement(oppositePlacement[currentPlacement] as Placement);
    }
  };

  return (
    <>
      {typeof renderToggle === 'function' ? renderToggle({ onToggle, toggleTarget }) : (
        <ToggleDropdown onClick={onToggle}
                        ref={toggleTarget}
                        role="presentation">
          {toggleChild}
        </ToggleDropdown>
      )}
      {show && (
        <Overlay show={show}
                 container={menuContainer}
                 containerPadding={10}
                 placement={currentPlacement}
                 shouldUpdatePosition
                 rootClose
                 onHide={onToggle}
                 target={() => toggleTarget.current}
                 transition={Transition}
                 onEntering={handleOverlayEntering}>
          <FilterProps>
            <DropdownMenu show={show}
                          onMenuItemSelect={closeOnSelect ? onToggle : undefined}
                          zIndex={dropdownZIndex}
                          minWidth={dropdownMinWidth}>
              {children}
            </DropdownMenu>
          </FilterProps>
        </Overlay>
      )}
    </>
  );
};

OverlayDropdown.propTypes = {
  children: PropTypes.node.isRequired,
  closeOnSelect: PropTypes.bool,
  dropdownZIndex: PropTypes.number,
  menuContainer: PropTypes.object,
  onToggle: PropTypes.func.isRequired,
  placement: PropTypes.string,
  show: PropTypes.bool.isRequired,
  toggleChild: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
};

OverlayDropdown.defaultProps = {
  closeOnSelect: true,
  dropdownMinWidth: undefined,
  dropdownZIndex: undefined,
  menuContainer: document.body,
  placement: 'bottom',
  renderToggle: undefined,
  toggleChild: 'Toggle',
};

export default OverlayDropdown;
