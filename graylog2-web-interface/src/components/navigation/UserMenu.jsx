import PropTypes from 'prop-types';
import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

import { NavDropdown, MenuItem } from 'components/graylog';
import { Icon } from 'components/common';

import Routes from 'routing/Routes';
import history from 'util/History';

const SessionStore = StoreProvider.getStore('Session');
const SessionActions = ActionsProvider.getActions('Session');

class UserMenu extends React.Component {
  static propTypes = {
    loginName: PropTypes.string.isRequired,
    fullName: PropTypes.string.isRequired,
  };

  onLogoutClicked = () => {
    SessionActions.logout.triggerPromise(SessionStore.getSessionId()).then(() => {
      history.push(Routes.STARTPAGE);
    });
  };

  render() {
    const { fullName, loginName } = this.props;

    return (
      <NavDropdown title={<Icon name="user" size="lg" aria-label={fullName} />}
                   id="user-menu-dropdown"
                   noCaret>
        <MenuItem header>{fullName}</MenuItem>
        <MenuItem divider />
        <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.USERS.edit(encodeURIComponent(loginName))}>
          <MenuItem>Edit profile</MenuItem>
        </LinkContainer>
        <MenuItem onSelect={this.onLogoutClicked}><Icon name="sign-out" /> Log out</MenuItem>
      </NavDropdown>
    );
  }
}

export default UserMenu;
