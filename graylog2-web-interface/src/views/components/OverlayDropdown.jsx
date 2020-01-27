import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { debounce } from 'lodash';

import { Overlay } from 'react-overlays';

import styles from './OverlayDropdown.css';

const FilterProps = ({ children, style }) => React.Children.map(children,
  child => React.cloneElement(child, { style: Object.assign({}, style, child.props.style) }));

const OverlayDropdown = ({ children, menuContainer, onToggle, placement, show, toggle, minDropdownWidth }) => {
  const [currentPlacement, setCurrentPlacement] = useState(placement);
  const [bodyWidth, setBodyWidth] = useState(document.body.clientWidth);
  const StyledList = styled.ul`
    padding-left: 5px;
    padding-right: 5px;
    color: #666666;
    z-index: 1050;
    min-width: ${minDropdownWidth};
    display: ${show ? 'block' : 'none'};
  `;

  const toggleTarget = React.createRef();
  const oppositePlacement = {
    left: 'right',
    right: 'left',
  };

  const handleToggle = (event) => {
    if (event.clientX + minDropdownWidth >= bodyWidth) {
      setCurrentPlacement(oppositePlacement[currentPlacement]);
      onToggle(event);
    } else {
      setCurrentPlacement(placement);
      onToggle(event);
    }
  };

  const handleBrowserResize = debounce(() => {
    setBodyWidth(document.body.clientWidth);
  }, 400);

  useEffect(() => {
    window.addEventListener('resize', handleBrowserResize, false);

    return () => {
      window.removeEventListener('resize', handleBrowserResize);
    };
  }, []);

  return (
    <>
      <span onClick={handleToggle}
            ref={toggleTarget}
            role="presentation"
            className={styles.dropdowntoggle}>
        {toggle}<span className="caret" />
      </span>
      <Overlay show={show}
               container={menuContainer}
               containerPadding={10}
               placement={currentPlacement}
               shouldUpdatePosition
               rootClose
               onHide={handleToggle}
               target={() => toggleTarget.current}>
        <FilterProps>
          <StyledList className="dropdown-menu">
            {children}
          </StyledList>
        </FilterProps>
      </Overlay>
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
  minDropdownWidth: PropTypes.oneOfType([PropTypes.string, PropTypes.string]),
};

OverlayDropdown.defaultProps = {
  menuContainer: document.body,
  placement: 'bottom',
  minDropdownWidth: 225,
};

export default OverlayDropdown;
