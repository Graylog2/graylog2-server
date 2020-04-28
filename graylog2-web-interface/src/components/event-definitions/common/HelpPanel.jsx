import React from 'react';
import PropTypes from 'prop-types';

import { Panel } from 'components/graylog';
import { Icon } from 'components/common';

import styles from './HelpPanel.css';

const ConditionalWrapper = ({ condition, wrapper, children }) => (condition ? wrapper(children) : children);

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
    const defaultHeader = (<h3><Icon name="info-circle" />&emsp;{title}</h3>);

    return (
      <Panel defaultExpanded={defaultExpanded}
             className={`${styles.helpPanel} ${className}`}
             bsStyle={bsStyle}>
        <Panel.Heading>
          <Panel.Title toggle={collapsible}>
            {header || defaultHeader}
          </Panel.Title>
        </Panel.Heading>
        <ConditionalWrapper condition={collapsible} wrapper={(wrapChild) => <Panel.Collapse>{wrapChild}</Panel.Collapse>}>
          <Panel.Body>
            {children}
          </Panel.Body>
        </ConditionalWrapper>
      </Panel>

    );
  }
}

export default HelpPanel;
