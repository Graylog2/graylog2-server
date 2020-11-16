/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';

import { Button } from 'components/graylog';
import ExternalLink from 'components/common/ExternalLink';

/**
 * Component that renders a link to an external resource as a button.
 *
 * All props besides `iconClass` and `children` are passed down to the react-bootstrap `<Button />` component.
 */

const ExternalLinkButton = ({ iconClass, children, ...props }) => {
  return (
    <Button {...props}>
      <ExternalLink iconClass={iconClass}>{children}</ExternalLink>
    </Button>
  );
};

ExternalLinkButton.propTypes = {
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

ExternalLinkButton.defaultProps = {
  bsStyle: 'default',
  bsSize: undefined,
  target: '_blank',
  iconClass: 'external-link-alt',
  className: '',
  disabled: false,
};

export default ExternalLinkButton;
