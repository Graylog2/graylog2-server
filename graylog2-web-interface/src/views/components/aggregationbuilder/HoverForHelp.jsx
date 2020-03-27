import React, { createRef, Component } from 'react';
import PropTypes from 'prop-types';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';

import { Popover } from 'components/graylog';
import { Icon } from 'components/common';

class HoverForHelp extends Component {
  constructor(props) {
    super(props);

    this.state = {
      hover: false,
    };

    this.hoverTarget = createRef();
  }

  _onToggleHover = () => this.setState(({ hover }) => ({ hover: !hover }));

  _renderPopover = () => {
    const { hover } = this.state;
    const { children, title } = this.props;

    if (!hover) {
      return null;
    }

    return (
      <Portal>
        <Position container={document.body}
                  placement="bottom"
                  target={this.hoverTarget.current}>
          <Popover title={title} id="configuration-popover">
            {children}
          </Popover>
        </Position>
      </Portal>
    );
  }

  render() {
    return (
      <span onMouseEnter={this._onToggleHover}
            onMouseLeave={this._onToggleHover}
            ref={this.hoverTarget}
            className="pull-right">
        <Icon name="question-circle" />
        {this._renderPopover()}
      </span>
    );
  }
}

HoverForHelp.propTypes = {
  children: PropTypes.any.isRequired,
  title: PropTypes.string.isRequired,
};

export default HoverForHelp;
