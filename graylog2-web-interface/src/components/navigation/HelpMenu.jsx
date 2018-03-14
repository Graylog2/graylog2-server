import PropTypes from 'prop-types';
import React from 'react';
import { NavDropdown, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import { ExternalLink } from 'components/common';

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

class HelpMenu extends React.Component {
  static propTypes = {
    active: PropTypes.bool.isRequired,
  };

  render() {
    return (
      <NavDropdown title="Help" id="help-menu-dropdown" active={this.props.active}>
        <LinkContainer to={Routes.getting_started(true)}>
          <MenuItem>Getting Started</MenuItem>
        </LinkContainer>
        <MenuItem href={DocsHelper.versionedDocsHomePage()} target="_blank">
          <ExternalLink>Documentation</ExternalLink>
        </MenuItem>
      </NavDropdown>
    );
  }
}

export default HelpMenu;
