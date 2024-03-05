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

import { Button } from 'components/bootstrap';
import ExternalLink from 'components/common/ExternalLink';

/**
 * Component that renders a link to an external resource as a button.
 *
 * All props besides `iconName` and `children` are passed down to the react-bootstrap `<Button />` component.
 */

type Props = React.ComponentProps<typeof Button> & {
  iconName: React.ComponentProps<typeof ExternalLink>['iconName']
};
const ExternalLinkButton = ({ iconName, children, ...props }: Props) => (
  <Button {...props}>
    <ExternalLink iconName={iconName}>{children}</ExternalLink>
  </Button>
);

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
  /** Icon name to use for the indicator icon. */
  iconName: PropTypes.string,
  /** Additional class name to adjust styling of the button. */
  className: PropTypes.string,
  /** Render a disabled button if this is <code>true</code>. */
  disabled: PropTypes.bool,
};

ExternalLinkButton.defaultProps = {
  bsStyle: 'default',
  bsSize: undefined,
  target: '_blank',
  iconName: 'open_in_new',
  className: '',
  disabled: false,
};

export default ExternalLinkButton;
