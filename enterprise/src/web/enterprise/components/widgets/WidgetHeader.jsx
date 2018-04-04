import React from 'react';
import PropTypes from 'prop-types';
import { Dropdown, MenuItem } from 'react-bootstrap';

import styles from './WidgetHeader.css';

const WidgetHeader = ({ children, editing, onDelete, onDuplicate, onToggleEdit, title }) => (
  <div className={styles.widgetHeader}>
    <i className={`fa fa-bars widget-drag-handle ${styles.widgetDragHandle}`} />{' '}{title}
    {children}
    <span className={`pull-right ${styles.widgetActionDropdown}`}>
      <Dropdown componentClass="span" id="widget-action-dropdown">
        <span bsRole="toggle">
          <i className={`fa fa-caret-down ${styles.widgetActionDropdownCaret}`} />
        </span>
        <Dropdown.Menu className={styles.widgetActionDropdownMenu}>
          <MenuItem header>Actions</MenuItem>
          <MenuItem onSelect={onToggleEdit}>{editing ? 'Finish Editing' : 'Edit'}</MenuItem>
          <MenuItem>Rename</MenuItem>
          <MenuItem onSelect={onDuplicate}>Duplicate</MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={onDelete}>Delete</MenuItem>
        </Dropdown.Menu>
      </Dropdown>
    </span>
  </div>
);

WidgetHeader.propTypes = {
  children: PropTypes.node,
  editing: PropTypes.bool,
  onDelete: PropTypes.func.isRequired,
  onDuplicate: PropTypes.func.isRequired,
  onToggleEdit: PropTypes.func.isRequired,
  title: PropTypes.node.isRequired,
};

WidgetHeader.defaultProps = {
  children: null,
  editing: false,
};

export default WidgetHeader;
