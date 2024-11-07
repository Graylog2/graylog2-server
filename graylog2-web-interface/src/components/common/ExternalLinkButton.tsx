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

import { Button } from 'components/bootstrap';
import ExternalLink from 'components/common/ExternalLink';

/**
 * Component that renders a link to an external resource as a button.
 *
 * All props besides `iconName` and `children` are passed down to the react-bootstrap `<Button />` component.
 */

type Props = React.ComponentProps<typeof Button> & {
  iconName?: React.ComponentProps<typeof ExternalLink>['iconName']
};
const ExternalLinkButton = ({ bsStyle = 'default', target = '_blank', className = '', disabled = false, iconName = 'open_in_new', children, ...props }: Props) => (
  <Button bsStyle={bsStyle} target={target} className={className} disabled={disabled} {...props}>
    <ExternalLink iconName={iconName}>{children}</ExternalLink>
  </Button>
);

export default ExternalLinkButton;
