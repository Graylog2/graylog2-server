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
import PropTypes from 'prop-types';
import React from 'react';

import { LinkContainer } from 'components/graylog/router';
import { MenuItem, NavDropdown } from 'components/graylog';
import { ExternalLink, Icon } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

const HelpMenu = ({ active }) => (
  <NavDropdown active={active}
               id="help-menu-dropdown"
               title={<Icon name="question-circle" size="lg" />}
               aria-label="Help"
               noCaret>

    <LinkContainer to={Routes.getting_started(true)}>
      <MenuItem>Getting Started</MenuItem>
    </LinkContainer>

    <MenuItem href={DocsHelper.versionedDocsHomePage()} target="_blank">
      <ExternalLink>Documentation</ExternalLink>
    </MenuItem>
  </NavDropdown>
);

HelpMenu.propTypes = {
  active: PropTypes.bool.isRequired,
};

export default HelpMenu;
