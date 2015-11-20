import React, {PropTypes} from 'react';
import { NavDropdown, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

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
        <MenuItem href="http://docs.graylog.org" target="blank">
          <i className="fa fa-external-link"></i>
          &nbsp;
          Documentation
        </MenuItem>
        <MenuItem divider/>
        <MenuItem href="https://www.graylog.org/support/" target="blank">
          <i className="fa fa-external-link"></i>
          &nbsp;
          Get support
        </MenuItem>
      </NavDropdown>
    );
  },
});

export default HelpMenu;
