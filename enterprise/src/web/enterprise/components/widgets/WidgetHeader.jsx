import React from 'react';
import PropTypes from 'prop-types';

import EditableTitle from 'enterprise/components/common/EditableTitle';
import styles from './WidgetHeader.css';

const WidgetHeader = ({ children, onRename, title }) => (
  <div className={styles.widgetHeader}>
    <i className={`fa fa-bars widget-drag-handle ${styles.widgetDragHandle}`} />{' '}
    <EditableTitle disabled={!onRename} value={title} onChange={onRename} />
    <span className={`pull-right ${styles.widgetActionDropdown}`}>
      {children}
    </span>
  </div>
);

WidgetHeader.propTypes = {
  children: PropTypes.node,
  onRename: PropTypes.func,
  title: PropTypes.node.isRequired,
};

WidgetHeader.defaultProps = {
  children: null,
  editing: false,
  onRename: undefined,
};

export default WidgetHeader;
