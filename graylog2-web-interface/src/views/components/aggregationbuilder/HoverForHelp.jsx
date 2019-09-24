import React from 'react';
import PropTypes from 'prop-types';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';

import { Popover } from 'components/graylog';
import { Icon } from 'components/common';

class HoverForHelp extends React.Component {
  state = {
    hover: false,
  };

  _onToggleHover = () => this.setState(({ hover }) => ({ hover: !hover }));

  render() {
    const { children, title } = this.props;
    const { hover } = this.state;
    const popover = hover && (
      <Portal>
        <Position container={document.body}
                  placement="bottom"
                  target={this.target}>
          <Popover title={title} id="configuration-popover">
            {children}
          </Popover>
        </Position>
      </Portal>
    );

    return (
      <span onMouseEnter={this._onToggleHover} onMouseLeave={this._onToggleHover}>
        <Icon name="question-circle" className="pull-right" ref={(elem) => { this.target = elem; }} />
        {popover}
      </span>
    );
  }
}

HoverForHelp.propTypes = {
  children: PropTypes.any.isRequired,
  title: PropTypes.string.isRequired,
};

export default HoverForHelp;
