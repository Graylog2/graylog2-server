import React from 'react';
import { Row, Col } from 'react-bootstrap';
import Routes from 'routing/Routes';

import UserNotification from 'util/UserNotification';
import history from 'util/History';

import StoreProvider from 'injection/StoreProvider';
const RolesStore = StoreProvider.getStore('Roles');
const UsersStore = StoreProvider.getStore('Users');

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import NewUserForm from 'components/users/NewUserForm';

class CreateUsersPage extends React.Component {
  state = {
    roles: undefined,
  };

  componentDidMount() {
    RolesStore.loadRoles().then((roles) => {
      this.setState({ roles: roles });
    });
  }

  _onSubmit = (r) => {
    const request = r;
    request.permissions = [];
    delete request['session-timeout-never'];
    UsersStore.create(request).then(() => {
      UserNotification.success(`User ${request.username} was created successfully.`, 'Success!');
      history.replace(Routes.SYSTEM.AUTHENTICATION.USERS.LIST);
    }, () => {
      UserNotification.error('Failed to create user!', 'Error!');
    });
  };

  _onCancel = () => {
    history.push(Routes.SYSTEM.AUTHENTICATION.USERS.LIST);
  };

  render() {
    if (!this.state.roles) {
      return <Spinner />;
    }
    return (
      <DocumentTitle title="Create new user">
        <span>
          <PageHeader title="Create new user" subpage>
            <span>
              Use this page to create new Graylog users. The users and their permissions created here are not limited
              to the web interface but valid and required for the REST APIs of your Graylog server nodes, too.
            </span>
          </PageHeader>
          <Row>
            <Col lg={8}>
              <NewUserForm roles={this.state.roles} onSubmit={this._onSubmit} onCancel={this._onCancel} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default CreateUsersPage;
