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
import PropTypes from 'prop-types';

import UsersDomain from 'domainActions/users/UsersDomain';
import { isPermitted } from 'util/PermissionsMixin';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import connect from 'stores/connect';

import EmailNotificationForm from './EmailNotificationForm';

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
      pagination: { page: 1, perPage: 50, query: '' },
    };
  }

  loadUsersPaginated = async (search, prevOptions) => {
    const { pagination: { page, perPage, query } } = this.state;
    const { currentUser } = this.props;
    let options = [];
    let hasMore;

    if (isPermitted(currentUser.permissions, 'users:list')) {
      const isNewQuery = search && search !== query;
      const pageParam = isNewQuery ? 1 : page;
      const response = await UsersDomain.loadUsersPaginated({ page: pageParam, perPage, query: search });

      const { pagination, list: usersList } = response;
      options = usersList.map((user) => ({ label: `${user.username} (${user.fullName})`, value: user.username })).toArray();

      this.setState({ pagination: { ...pagination, page: page + 1 } });
      hasMore = prevOptions.length < pagination.total;
    }

    return {
      options: options,
      hasMore,
    };
  };

  render() {
    return <EmailNotificationForm {...this.props} users={[]} loadUsers={this.loadUsersPaginated} />;
  }
}

export default connect(EmailNotificationFormContainer,
  { currentUser: CurrentUserStore },
  ({ currentUser }) => ({ currentUser: currentUser ? currentUser.currentUser : currentUser }));
