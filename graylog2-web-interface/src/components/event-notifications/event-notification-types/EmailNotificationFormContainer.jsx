import * as React from 'react';
import Immutable from 'immutable';
import PropTypes from 'prop-types';

import UsersDomain from 'domainActions/users/UsersDomain';
import { Spinner } from 'components/common';
import { isPermitted } from 'util/PermissionsMixin';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';

import EmailNotificationForm from './EmailNotificationForm';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

class EmailNotificationFormContainer extends React.Component {
  static propTypes = {
    currentUser: PropTypes.object.isRequired,
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      users: undefined,
    };
  }

  componentDidMount() {
    this.loadUsers();
  }

  loadUsers = () => {
    const { currentUser } = this.props;

    if (isPermitted(currentUser.permissions, 'users:list')) {
      UsersDomain.loadUsers().then((users) => this.setState({ users }));
    } else {
      this.setState({ users: Immutable.List() });
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
