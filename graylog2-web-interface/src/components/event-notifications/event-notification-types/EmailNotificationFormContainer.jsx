import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';
import PermissionsMixin from 'util/PermissionsMixin';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';

import EmailNotificationForm from './EmailNotificationForm';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');
const { UsersStore } = CombinedProvider.get('Users');

class EmailNotificationFormContainer extends React.Component {
  static propTypes = {
    currentUser: PropTypes.object.isRequired,
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  state = {
    users: undefined,
  };

  componentDidMount() {
    this.loadUsers();
  }

  loadUsers = () => {
    const { currentUser } = this.props;

    if (PermissionsMixin.isPermitted(currentUser.permissions, 'users:list')) {
      UsersStore.loadUsers()
        .then((users) => {
          this.setState({ users: users });
        });
    } else {
      this.setState({ users: [] });
    }
  };

  render() {
    const { users } = this.state;

    if (!users) {
      return <p><Spinner text="Loading Notification information..." /></p>;
    }

    return <EmailNotificationForm {...this.props} users={users} />;
  }
}

export default connect(EmailNotificationFormContainer,
  { currentUser: CurrentUserStore },
  ({ currentUser }) => ({ currentUser: currentUser ? currentUser.currentUser : currentUser }));
