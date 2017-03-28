import React, { PropTypes } from 'react';
import { NavDropdown, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

const HelpMenu = React.createClass({
  propTypes: {
    active: PropTypes.bool.isRequired,
  },
  render() {
    return (
      <NavDropdown title="Help" id="help-menu-dropdown" active={this.props.active}>
        <LinkContainer to={Routes.getting_started(true)}>
          <MenuItem>Getting Started</MenuItem>
        </LinkContainer>
        <MenuItem href={DocsHelper.versionedDocsHomePage()} target="blank">
          <i className="fa fa-external-link" /> Documentation
        </MenuItem>
      </NavDropdown>
    );
  },
});

export default HelpMenu;
