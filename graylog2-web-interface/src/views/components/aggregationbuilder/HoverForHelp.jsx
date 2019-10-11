import React from 'react';
import PropTypes from 'prop-types';
import { Popover } from 'components/graylog';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';

class HoverForHelp extends React.Component {
  state = {
    hover: false,
  };

  _onToggleHover = () => this.setState(({ hover }) => ({ hover: !hover }));

  render() {
    const { children, title } = this.props;
    const popover = this.state.hover ? (
      <Portal>
        <Position container={document.body}
                  placement="bottom"
                  target={this.target}>
          <Popover title={title} id="configuration-popover">
            {children}
          </Popover>
        </Position>
      </Portal>
    ) : null;
    return (
      <span onMouseEnter={this._onToggleHover} onMouseLeave={this._onToggleHover}>
        <i className="fa fa-question-circle pull-right" ref={(elem) => { this.target = elem; }} />
        {popover}
      </span>
    );
  }
}

HoverForHelp.propTypes = {};

export default HoverForHelp;
