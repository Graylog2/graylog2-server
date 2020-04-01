import PropTypes from 'prop-types';
import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

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
