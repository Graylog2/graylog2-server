import React from 'react';
import PropTypes from 'prop-types';
import { Dropdown, MenuItem } from 'react-bootstrap';

import styles from './DashboardWidgetHeader.css';

const DashboardWidgetHeader = ({ children, onDelete, title }) => (
  <div className={styles.widgetHeader}>
    <i className={`fa fa-bars widget-drag-handle ${styles.widgetDragHandle}`} />{' '}
    {title}
    {children}
    <span className={`pull-right ${styles.widgetActionDropdown}`}>
      <Dropdown componentClass="span" id="widget-action-dropdown">
        <span bsRole="toggle">
          <i className={`fa fa-caret-down ${styles.widgetActionDropdownCaret}`} />
        </span>
        <Dropdown.Menu className={styles.widgetActionDropdownMenu}>
          <MenuItem header>Actions</MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={onDelete}>Delete</MenuItem>
        </Dropdown.Menu>
      </Dropdown>
    </span>
  </div>
);

DashboardWidgetHeader.propTypes = {
  children: PropTypes.node,
  onDelete: PropTypes.func.isRequired,
  title: PropTypes.node.isRequired,
};

DashboardWidgetHeader.defaultProps = {
  children: null,
};

export default DashboardWidgetHeader;
