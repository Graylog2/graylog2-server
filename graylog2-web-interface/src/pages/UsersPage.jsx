import React from 'react';
import PropTypes from 'prop-types';
import { inject, observer } from 'mobx-react';
import { Row, Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import PermissionsMixin from 'util/PermissionsMixin';
import Routes from 'routing/Routes';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import { DocumentTitle, IfPermitted, PageHeader } from 'components/common';
import UserList from 'components/users/UserList';

const UsersPage = React.createClass({
  propTypes: {
    currentUser: PropTypes.object.isRequired,
  },

  mixins: [PermissionsMixin],
  render() {
    return (
      <DocumentTitle title="Users">
        <span>
          <PageHeader title="User accounts" subpage>
            <span>Create as many users as you want next to the default administrator user here. You can also make changes to already existing users.</span>
            {null}
            <span>
              <IfPermitted permissions="users:edit">
                <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.USERS.CREATE}>
                  <Button bsStyle="success">Add new user</Button>
                </LinkContainer>
              </IfPermitted>
            </span>
          </PageHeader>

          <Row>
            <Col md={12}>
              <UserList currentUsername={this.props.currentUser.username} currentUser={this.props.currentUser} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default inject(() => ({
  currentUser: CurrentUserStore.currentUser,
}))(observer(UsersPage));
