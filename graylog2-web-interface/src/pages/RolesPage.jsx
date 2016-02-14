import React from 'react';
import { Row, Col } from 'react-bootstrap';

import DocsHelper from 'util/DocsHelper';

import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';
import RolesComponent from 'components/users/RolesComponent';

const RolesPage = React.createClass({
  render() {
    return (
      <span>
        <PageHeader title="Roles">
          <span>
            Roles bundle permissions which can be assigned to multiple users at once
          </span>

          <span>
            Read more about Graylog roles in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES} text="documentation"/>.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <RolesComponent />
          </Col>
        </Row>
      </span>
    );
  },
});

export default RolesPage;
