import React from 'react';
import PropTypes from 'prop-types';
import { Dropdown, MenuItem } from 'react-bootstrap';

import styles from './WidgetActionDropdown.css';

/**
 * This implements a custom toggle for a dropdown menu.
 * See: "Custom Dropdown Components" in react-bootstrap documentation.
 */
const WidgetActionToggle = ({ children, onClick }) => {
  const handleClick = (e) => {
    e.preventDefault();
    onClick(e);
  };

  return (
    <span onClick={handleClick} role="presentation">
      {children}
    </span>
  );
};

WidgetActionToggle.propTypes = {
  children: PropTypes.node.isRequired,
  onClick: PropTypes.func,
};

WidgetActionToggle.defaultProps = {
  onClick: () => {},
};

const WidgetActionDropdown = ({ children, element }) => (
  <Dropdown componentClass="span" id="widget-action-dropdown">
    <WidgetActionToggle bsRole="toggle">
      {element}
    </WidgetActionToggle>
    <Dropdown.Menu className={styles.widgetActionDropdownMenu}>
      <MenuItem header>Actions</MenuItem>
      {children}
    </Dropdown.Menu>
  </Dropdown>
);


WidgetActionDropdown.propTypes = {
  children: PropTypes.node.isRequired,
  element: PropTypes.node.isRequired,
};

WidgetActionDropdown.defaultProps = {
};

export default WidgetActionDropdown;
