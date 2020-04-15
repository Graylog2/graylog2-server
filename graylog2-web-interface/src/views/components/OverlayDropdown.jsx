import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Overlay, Transition } from 'react-overlays';

import styles from './OverlayDropdown.css';

const StyledList = styled.ul(({ show }) => `
  padding-left: 5px;
  padding-right: 5px;
  color: #666666;
  z-index: 1050;
  min-width: 'max-content';
  display: ${show ? 'block' : 'none'};
`);

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
      <span onClick={onToggle}
            ref={toggleTarget}
            role="presentation"
            className={styles.dropdowntoggle}>
        {toggle}<span className="caret" />
      </span>
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
            <StyledList className="dropdown-menu"
                        show={show}>
              {children}
            </StyledList>
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
