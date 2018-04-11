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

const WidgetActionDropdown = ({ children, editing, onAddToDashboard, onDelete, onDuplicate, onToggleEdit }) => (
  <Dropdown componentClass="span" id="widget-action-dropdown">
    <WidgetActionToggle bsRole="toggle">
      {children}
    </WidgetActionToggle>
    <Dropdown.Menu className={styles.widgetActionDropdownMenu}>
      <MenuItem header>Actions</MenuItem>
      <MenuItem onSelect={onToggleEdit}>{editing ? 'Finish Editing' : 'Edit'}</MenuItem>
      <MenuItem onSelect={onDuplicate}>Duplicate</MenuItem>
      <MenuItem divider />
      <MenuItem onSelect={onAddToDashboard}>Add to dashboard</MenuItem>
      <MenuItem divider />
      <MenuItem onSelect={onDelete}>Delete</MenuItem>
    </Dropdown.Menu>
  </Dropdown>
);


WidgetActionDropdown.propTypes = {
  children: PropTypes.node.isRequired,
  editing: PropTypes.bool,
  onAddToDashboard: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  onDuplicate: PropTypes.func.isRequired,
  onToggleEdit: PropTypes.func.isRequired,
};

WidgetActionDropdown.defaultProps = {
  editing: false,
};

export default WidgetActionDropdown;
