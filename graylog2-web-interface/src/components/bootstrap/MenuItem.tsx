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

const Container = styled.div`
  display: flex;
`;

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

const MenuItem = ({ className, children, icon, ...props } : Props) => {
  return (
    <BootstrapMenuItem bsClass={className} {...props}>
      {children && (
        <Container>
          {icon && <IconWrapper><Icon name={icon} /></IconWrapper>}
          {children}
        </Container>
      )}
    </BootstrapMenuItem>
  );
};

MenuItem.propTypes = {
  className: PropTypes.string,
  icon: PropTypes.string,
};

MenuItem.defaultProps = {
  className: undefined,
  icon: undefined,
};

/** @component */
export default MenuItem;
