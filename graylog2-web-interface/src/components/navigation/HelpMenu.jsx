import React, {PropTypes} from 'react';
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
      <NavDropdown navItem title="Help" id="user-menu-dropdown" active={this.props.active}>
        <MenuItem href="http://info.graylog.org/report-a-problem" target="_blank">
          <i className="fa fa-external-link"></i> Report a Problem
        </MenuItem>
        <MenuItem href="http://info.graylog.org/ask-a-question" target="_blank">
          <i className="fa fa-external-link"></i> Ask a Question
        </MenuItem>

        <MenuItem divider />
        <LinkContainer to={Routes.getting_started(true)}>
          <MenuItem>Getting Started</MenuItem>
        </LinkContainer>
        <MenuItem href={DocsHelper.versionedDocsHomePage()} target="blank">
          <i className="fa fa-external-link"></i> Documentation
        </MenuItem>
      </NavDropdown>
    );
  },
});

export default HelpMenu;
