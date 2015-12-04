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
        <LinkContainer to={Routes.GETTING_STARTED}>
          <MenuItem>Getting Started</MenuItem>
        </LinkContainer>
        <MenuItem href={DocsHelper.versionedDocsHomePage()} target="blank">
          <i className="fa fa-external-link"></i>
          &nbsp;
          Documentation
        </MenuItem>
        <MenuItem divider/>
        <MenuItem href="https://www.graylog.org/support/" target="blank">
          <i className="fa fa-external-link"></i>
          &nbsp;
          Get Support
        </MenuItem>
        <MenuItem href="https://graylog.wufoo.com/forms/qup3ebj0kp9cfo/" target="blank">
          <i className="fa fa-external-link"></i>
          &nbsp;
          Feedback
        </MenuItem>
        <MenuItem divider/>
        <MenuItem href="https://graylog.wufoo.com/forms/mzwusin1f7kudv/" target="blank">
          <i className="fa fa-external-link"></i>
          &nbsp;
          Ask a Question
        </MenuItem>
        <MenuItem href="https://graylog.wufoo.com/forms/zaw0hgh07cndha/" target="blank">
          <i className="fa fa-external-link"></i>
          &nbsp;
          Report a Problem
        </MenuItem>
        <MenuItem href="https://graylog.ideas.aha.io/?sort=popular" target="blank">
          <i className="fa fa-external-link"></i>
          &nbsp;
          Product Ideas
        </MenuItem>
      </NavDropdown>
    );
  },
});

export default HelpMenu;
