import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { NavDropdown, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { actions } from 'ducks/sessions';

const UserMenu = React.createClass({
  propTypes: {
    loginName: PropTypes.string.isRequired,
    fullName: PropTypes.string.isRequired,
    logout: PropTypes.func,
  },
  onLogoutClicked() {
    this.props.logout();
  },
  render() {
    return (
      <NavDropdown title={this.props.fullName} id="user-menu-dropdown">
        <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.USERS.edit(encodeURIComponent(this.props.loginName))}>
          <MenuItem>Edit profile</MenuItem>
        </LinkContainer>
        <MenuItem divider />
        <MenuItem onSelect={this.onLogoutClicked}><i className="fa fa-sign-out" /> Log out</MenuItem>
      </NavDropdown>
    );
  },
});

const mapDispatchToProps = dispatch => ({
  logout: () => dispatch(actions.logout()),
});

export default connect(() => ({}), mapDispatchToProps)(UserMenu);

