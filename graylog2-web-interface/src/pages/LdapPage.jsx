import React from 'react';
import { Row, Col } from 'react-bootstrap';

import DocsHelper from 'util/DocsHelper';

import { PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import LdapComponent from 'components/users/LdapComponent';

const LdapPage = React.createClass({
  render() {
    return (
      <span>
        <PageHeader title="LDAP Settings" titleSize={8} buttonSize={4} buttonStyle={{textAlign: 'right', marginTop: '10px'}}>
          <span>This page is the only resource you need to set up the Graylog LDAP integration. You can test the connection to your LDAP server and even try to log in with an LDAP account of your choice right away.</span>

          <span>Read more about LDAP configuration in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES} text="documentation"/>.</span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <LdapComponent />
          </Col>
        </Row>
      </span>
    );
  },
});

export default LdapPage;
