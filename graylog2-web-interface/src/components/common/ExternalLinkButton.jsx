import React from 'react';
import PropTypes from 'prop-types';
import { Button } from 'react-bootstrap';

import { ExternalLink } from 'components/common';

/**
 * Component that renders a link to an external resource as a button.
 *
 * All props besides `iconClass` and `children` are passed down to the react-bootstrap `<Button />` component.
 */
class ExternalLinkButton extends React.Component {
  static propTypes = {
    /** Link to the external location. */
    href: PropTypes.string.isRequired,
    /** Text for the button. (should be one line) */
    children: PropTypes.node.isRequired,
    /** Button style. (bootstrap style name) */
    bsStyle: PropTypes.string,
    /** Button size. (bootstrap size name) */
    bsSize: PropTypes.string,
    /** Browser window target attribute for the external link. */
    target: PropTypes.string,
    /** FontAwesome icon class name to use for the indicator icon. */
    iconClass: PropTypes.string,
    /** Additional class name to adjust styling of the button. */
    className: PropTypes.string,
    /** Render a disabled button if this is <code>true</code>. */
    disabled: PropTypes.bool,
  };

  static defaultProps = {
    bsStyle: 'default',
    bsSize: '',
    target: '_blank',
    iconClass: 'fa-external-link',
    className: '',
    disabled: false,
  };

  render() {
    const { iconClass, children, ...props } = this.props;

    return (
      <Button {...props}>
        <ExternalLink iconClass={iconClass}>{children}</ExternalLink>
      </Button>
    );
  }
}

export default ExternalLinkButton;
