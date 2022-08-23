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
// eslint-disable-next-line no-restricted-imports
import { MenuItem as BootstrapMenuItem } from 'react-bootstrap';
import styled from 'styled-components';

import Icon from 'components/common/Icon';

const IconWrapper = styled.div`
  display: inline-flex;
  min-width: 20px;
  margin-right: 5px;
  justify-content: center;
  align-items: center;
`;

type Props = React.ComponentProps<typeof BootstrapMenuItem> & {
  icon?: React.ComponentProps<typeof Icon>['name'],
}

const CustomMenuItem = ({ className, children, icon, ...props } : Props) => (
  <BootstrapMenuItem bsClass={className} {...props}>
    {children && (
      <>
        {icon && <IconWrapper><Icon name={icon} /></IconWrapper>}
        {children}
      </>
    )}
  </BootstrapMenuItem>
);

CustomMenuItem.propTypes = {
  className: PropTypes.string,
  icon: PropTypes.string,
};

CustomMenuItem.defaultProps = {
  className: undefined,
  icon: undefined,
};

/** @component */
export default CustomMenuItem;
