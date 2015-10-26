import React from 'react';
import { Row, Col } from 'react-bootstrap';

import RolesStore from 'stores/users/RolesStore';

import Spinner from 'components/common/Spinner';
import PageHeader from 'components/common/PageHeader';
import NewUserForm from 'components/users/NewUserForm';

const CreateUsersPage = React.createClass({
  componentDidMount() {
    RolesStore.loadRoles().then((response) => {
      this.setState({roles: response.roles});
    });
  },
  getInitialState() {
    return {
      roles: undefined,
    };
  },
  _onSubmit(evt) {
    console.log(evt);
  },
  render() {
    if (!this.state.roles) {
      return <Spinner />;
    }
    return (
      <span>
        <PageHeader title="Create new user">
          <span>
            Use this page to create new Graylog users. The users and their permissions created here are not limited
            to the web interface but valid and required for the REST APIs of your Graylog server nodes, too.
          </span>
        </PageHeader>
        <Row className="content">
          <Col lg={8}>
            <NewUserForm roles={this.state.roles} onSubmit={this._onSubmit}/>
          </Col>
        </Row>
      </span>
    );
  },
});

export default CreateUsersPage;
