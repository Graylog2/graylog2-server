import React from 'react';
import PropTypes from 'prop-types';

/**
 * This can be used with LinkContainer to avoid this annoying warning:
 *
 *   "Unknown prop `active` on <a> tag. Remove this prop from the element."
 *
 * See: https://github.com/react-bootstrap/react-bootstrap/issues/2304
 */
export default class Link extends React.Component {
  static propTypes = {
    active: PropTypes.bool,
    href: PropTypes.string,
    children: PropTypes.node.isRequired,
  };

  static defaultProps = {
    active: false,
    href: '#',
  };

  render() {
    // Do not pass the "active" prop to the <a/> element to avoid an annoying warning
    // See: https://github.com/react-bootstrap/react-bootstrap/issues/2304
    const { active, children, ...childProps } = this.props;

    return <a {...childProps}>{children}</a>;
  }
}
