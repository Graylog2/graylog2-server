/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
