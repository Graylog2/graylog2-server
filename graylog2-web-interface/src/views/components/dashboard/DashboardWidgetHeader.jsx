import React from 'react';
import PropTypes from 'prop-types';

import { Dropdown, MenuItem } from 'components/graylog';
import { Icon } from 'components/common';

import styles from './DashboardWidgetHeader.css';

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
    const { onClick } = this.props;

    e.preventDefault();
    onClick(e);
  };

  render() {
    return (
      <span onClick={this.handleClick} role="presentation">
        <Icon name="caret-down" className={styles.widgetActionDropdownCaret} />
      </span>
    );
  }
}

const DashboardWidgetHeader = ({ children, onDelete, title }) => (
  <div className={styles.widgetHeader}>
    <Icon name="bars" className={`widget-drag-handle ${styles.widgetDragHandle}`} />{' '}
    {title}
    {children}
    <span className={`pull-right ${styles.widgetActionDropdown}`}>
      <Dropdown componentClass="span" id="widget-action-dropdown">
        <WidgetActionToggle bsRole="toggle" />
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
