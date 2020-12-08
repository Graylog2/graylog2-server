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
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Overlay, Transition } from 'react-overlays';

import { DropdownMenu } from 'components/graylog';

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

const FilterProps = ({ children, style }) => React.Children.map(children,
  (child) => React.cloneElement(child, { style: { ...style, ...child.props.style } }));

const OverlayDropdown = ({ children, menuContainer, onToggle, placement, show, toggle }) => {
  const [currentPlacement, setCurrentPlacement] = useState(placement);
  const toggleTarget = React.createRef();

  const handleOverlayEntering = (dropdownElem) => {
    const dropdownOffsetLeft = dropdownElem.offsetLeft;
    const dropdownWidth = dropdownElem.offsetWidth;
    const overflowRight = dropdownOffsetLeft + dropdownWidth >= document.body.clientWidth;
    const overflowLeft = dropdownOffsetLeft < 0;
    const trimmedDropdown = (overflowLeft && currentPlacement === 'left') || (overflowRight && currentPlacement === 'right');

    if (trimmedDropdown) {
      setCurrentPlacement(oppositePlacement[currentPlacement]);
    }
  };

  return (
    <>
      <ToggleDropdown onClick={onToggle}
                      ref={toggleTarget}
                      role="presentation">
        {toggle}<span className="caret" />
      </ToggleDropdown>
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
            <DropdownMenu show={show}>
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
  menuContainer: PropTypes.object,
  onToggle: PropTypes.func.isRequired,
  placement: PropTypes.string,
  show: PropTypes.bool.isRequired,
  toggle: PropTypes.oneOfType([PropTypes.string, PropTypes.node]).isRequired,
};

OverlayDropdown.defaultProps = {
  menuContainer: document.body,
  placement: 'bottom',
};

export default OverlayDropdown;
