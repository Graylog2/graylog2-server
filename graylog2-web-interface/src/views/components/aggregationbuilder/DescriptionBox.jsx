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
    configurableOptions: PropTypes.node,
    description: PropTypes.string.isRequired,
    help: PropTypes.string,
    style: PropTypes.object,
  };

  static defaultProps = {
    configurableOptions: undefined,
    style: undefined,
    help: undefined,
  };

  constructor(props) {
    super(props);

    this.state = {
      configOpen: false,
    };
  }

  onToggleConfig = () => {
    const { configOpen } = this.state;
    this.setState({ configOpen: !configOpen });
  };

  configPopover = () => {
    const { configOpen } = this.state;
    const { configurableOptions } = this.props;

    if (!configOpen) {
      return '';
    }

    const configurableElement = React.cloneElement(configurableOptions, {
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
    const { configurableOptions } = this.props;
    if (configurableOptions) {
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
    const { description, children, help, style: inlineStyle } = this.props;
    return (
      <div className={styles.descriptionBox} style={inlineStyle}>
        <div className={styles.description}>{description} {this.configCaret()} {help && <HoverForHelp title={description}>{help}</HoverForHelp>}</div>
        {children}
        {this.configPopover()}
      </div>
    );
  }
}
