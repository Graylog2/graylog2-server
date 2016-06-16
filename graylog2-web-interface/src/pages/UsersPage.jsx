import React from 'react';
import Reflux from 'reflux';
import { Row, Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import DocsHelper from 'util/DocsHelper';
import PermissionsMixin from 'util/PermissionsMixin';
import Routes from 'routing/Routes';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';
import UserList from 'components/users/UserList';

const UsersPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore), PermissionsMixin],
  render() {
    const permissions = this.state.currentUser.permissions;
    // TODO: fix permission names
    return (
      <span>
        <PageHeader title="User accounts">
          <span>Create as many users as you want next to the default administrator user here. You can also configure LDAP and make changes to already existing users.</span>

          <span>Read more about user management in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES} text="documentation"/>.</span>
          <span>
            {this.isPermitted(permissions, 'ldap:edit') &&
              <LinkContainer to={Routes.SYSTEM.LDAP.SETTINGS}>
                <Button bsStyle="info">Configure LDAP</Button>
              </LinkContainer>
            }
            {' '}
            {this.isPermitted(permissions, 'ldapgroups:edit') &&
              <LinkContainer to={Routes.SYSTEM.LDAP.GROUPS}>
                <Button bsStyle="info">LDAP Group Mapping</Button>
              </LinkContainer>
            }
            {' '}
            {this.isPermitted(permissions, 'users:create') &&
              <LinkContainer to={Routes.SYSTEM.USERS.CREATE}>
                <Button bsStyle="success">Add new user</Button>
              </LinkContainer>
            }
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <UserList currentUsername={this.state.currentUser.username} currentUser={this.state.currentUser}/>
          </Col>
        </Row>
      </span>
    );
  },
});

export default UsersPage;
