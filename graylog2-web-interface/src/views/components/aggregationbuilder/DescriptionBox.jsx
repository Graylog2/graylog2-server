import React from 'react';
import PropTypes from 'prop-types';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';

import { Popover } from 'components/graylog';
import { Icon } from 'components/common';

import styles from './DescriptionBox.css';
import HoverForHelp from './HoverForHelp';

export default class DescriptionBox extends React.Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
    description: PropTypes.string.isRequired,
    configurableOptions: PropTypes.node,
  };

  static defaultProps = {
    configurableOptions: undefined,
  };

  constructor(props) {
    super(props);

    this.state = {
      configOpen: false,
    };
  }

  onToggleConfig = () => {
    this.setState({ configOpen: !this.state.configOpen });
  };

  configPopover = () => {
    if (!this.state.configOpen) {
      return '';
    }

    const configurableElement = React.cloneElement(this.props.configurableOptions, {
      onClose: this.onToggleConfig,
    });

    return (
      <Portal>
        <Position container={document.body}
                  placement="bottom"
                  target={this.target}>
          <Popover title="Config options" id="config-popover">
            {configurableElement}
          </Popover>
        </Position>
      </Portal>
    );
  };

  configCaret = () => {
    if (this.props.configurableOptions) {
      return (
        <Icon ref={(node) => { this.target = node; }}
              role="button"
              tabIndex={0}
              onClick={this.onToggleConfig}
              name="wrench" />
      );
    }
    return null;
  };

  render() {
    const { description, children, help } = this.props;
    return (
      <div className={styles.descriptionBox}>
        <div className={styles.description}>{description} {this.configCaret()} {help && <HoverForHelp title={description}>{help}</HoverForHelp>}</div>
        {children}
        {this.configPopover()}
      </div>
    );
  }
}
