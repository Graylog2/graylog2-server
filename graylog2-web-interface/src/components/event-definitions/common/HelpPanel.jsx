import React from 'react';
import PropTypes from 'prop-types';

import { Icon, Panel } from 'components/graylog';

import styles from './HelpPanel.css';

class HelpPanel extends React.Component {
  static propTypes = {
    bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary']),
    children: PropTypes.node,
    className: PropTypes.string,
    collapsible: PropTypes.bool,
    header: PropTypes.node,
    title: PropTypes.string,
    defaultExpanded: PropTypes.bool,
  };

  static defaultProps = {
    bsStyle: 'info',
    children: undefined,
    className: '',
    collapsible: false,
    header: undefined,
    title: '',
    defaultExpanded: false,
  };

  render() {
    const { bsStyle, children, className, collapsible, header, title, defaultExpanded } = this.props;
    const defaultHeader = (<h3><Icon className="fa fa-info-circle" />&emsp;{title}</h3>);

    return (
      <Panel bsStyle={bsStyle}
             className={`${styles.helpPanel} ${className}`}
             collapsible={collapsible}
             defaultExpanded={defaultExpanded}
             header={header || defaultHeader}>
        {children}
      </Panel>
    );
  }
}

export default HelpPanel;
