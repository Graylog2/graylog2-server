import React from 'react';
import PropTypes from 'prop-types';

import styles from './WidgetHeader.css';

const WidgetHeader = ({ children, title }) => (
  <div className="widget-title" style={{ marginBottom: '5px' }}>
    <i className={`fa fa-bars widget-drag-handle ${styles.widgetDragHandle}`} />{' '}{title}
    {children}
  </div>
);

WidgetHeader.propTypes = {
  children: PropTypes.node,
  title: PropTypes.node.isRequired,
};

WidgetHeader.defaultProps = {
  children: null,
};

export default WidgetHeader;
