import React from 'react';
import { OverlayTrigger } from 'react-bootstrap';

const OverlayElement = React.createClass({
  propTypes: {
    overlay: React.PropTypes.element,
    placement: React.PropTypes.oneOf(['top', 'bottom', 'right', 'left']),
    trigger: React.PropTypes.oneOfType([
      React.PropTypes.oneOf(['click', 'hover', 'focus']),
      React.PropTypes.arrayOf(React.PropTypes.oneOf(['click', 'hover', 'focus'])),
    ]),
    useOverlay: React.PropTypes.bool, // True if overlay should be applied, false otherwise
    children: React.PropTypes.oneOfType([
      React.PropTypes.arrayOf(React.PropTypes.element),
      React.PropTypes.element,
    ]).isRequired,
  },

  render() {
    if (this.props.overlay && this.props.useOverlay) {
      // We need to wrap the element in a span to be able to display overlays in disabled buttons and links
      // https://github.com/react-bootstrap/react-bootstrap/issues/364
      return (
        <OverlayTrigger placement={this.props.placement} trigger={this.props.trigger} overlay={this.props.overlay} rootClose>
          <span>
            {this.props.children}
          </span>
        </OverlayTrigger>
      );
    }

    return this.props.children;
  },
});

export default OverlayElement;
