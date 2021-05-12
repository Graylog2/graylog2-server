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
// eslint-disable-next-line no-restricted-imports
import { NavDropdown as BootstrapNavDropdown } from 'react-bootstrap';
import styled from 'styled-components';

import menuItemStyles from './styles/menuItem';

class ModifiedBootstrapNavDropdown extends BootstrapNavDropdown {
  // eslint-disable-next-line class-methods-use-this
  isActive({ props }, activeKey, activeHref) {
    // NOTE: had to override library as it doesn't respect setting `active={false}`
    if (props.active === false) {
      return false;
    }

    if (
      props.active
      || (activeKey != null && props.eventKey === activeKey)
      || (activeHref && props.href === activeHref)
    ) {
      return true;
    }

    return props.active;
  }
}

const NavDropdown = styled(BootstrapNavDropdown)`
  ${menuItemStyles}
`;

const ModifiedNavDropdown = styled(ModifiedBootstrapNavDropdown)`
  ${menuItemStyles}
`;

/** @component */
export default NavDropdown;
export { ModifiedNavDropdown };
