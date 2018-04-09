import React from 'react';
import PropTypes from 'prop-types';
import { Dropdown, MenuItem } from 'react-bootstrap';

import EditableTitle from 'enterprise/components/common/EditableTitle';
import styles from './WidgetHeader.css';

/**
 * This implements a custom toggle for a dropdown menu.
 * See: "Custom Dropdown Components" in react-bootstrap documentation.
 */
class WidgetActionToggle extends React.Component {
  static propTypes = {
    onClick: PropTypes.func,
  };

  static defaultProps = {
    onClick: () => {},
  };

  handleClick = (e) => {
    e.preventDefault();
    this.props.onClick(e);
  };

  render() {
    return (
      <span onClick={this.handleClick} role="presentation">
        <i className={`fa fa-caret-down ${styles.widgetActionDropdownCaret}`} />
      </span>
    );
  }
}

const WidgetHeader = ({ children, editing, onDelete, onDuplicate, onToggleEdit, onRename, onAddToDashboard, title }) => (
  <div className={styles.widgetHeader}>
    <i className={`fa fa-bars widget-drag-handle ${styles.widgetDragHandle}`} />{' '}
    <EditableTitle value={title} onChange={onRename} />
    {children}
    <span className={`pull-right ${styles.widgetActionDropdown}`}>
      <Dropdown componentClass="span" id="widget-action-dropdown">
        <WidgetActionToggle bsRole="toggle" />
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
    </span>
  </div>
);

WidgetHeader.propTypes = {
  children: PropTypes.node,
  editing: PropTypes.bool,
  onDelete: PropTypes.func.isRequired,
  onDuplicate: PropTypes.func.isRequired,
  onToggleEdit: PropTypes.func.isRequired,
  onAddToDashboard: PropTypes.func.isRequired,
  title: PropTypes.node.isRequired,
};

WidgetHeader.defaultProps = {
  children: null,
  editing: false,
};

export default WidgetHeader;
