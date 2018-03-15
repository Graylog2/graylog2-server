import PropTypes from 'prop-types';
import React from 'react';
import { OverlayTrigger } from 'react-bootstrap';

/**
 * Helper component for react-bootstrap's `OverlayTrigger`. It only wraps the content into a `span` element,
 * so that the overlay can be displayed in disabled buttons and links.
 *
 * See: https://github.com/react-bootstrap/react-bootstrap/issues/364
 */
class OverlayElement extends React.Component {
  static propTypes = {
    /** Element that will be displayed in the overlay. */
    overlay: PropTypes.element,
    /** Placement for the overlay. */
    placement: PropTypes.oneOf(['top', 'bottom', 'right', 'left']),
    /** Action that will trigger the overlay. */
    trigger: PropTypes.oneOfType([
      PropTypes.oneOf(['click', 'hover', 'focus']),
      PropTypes.arrayOf(PropTypes.oneOf(['click', 'hover', 'focus'])),
    ]),
    /** Use `false` to disable the overlay. */
    useOverlay: PropTypes.bool,
    /** Components to render. They will be wrapped in a `span` that will trigger the overlay element. */
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  };

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
  }
}

export default OverlayElement;
