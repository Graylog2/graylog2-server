import React from 'react';
import { Button, Row, Col } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

import { IfPermitted, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import LdapComponent from 'components/ldap/LdapComponent';

const LdapPage = React.createClass({
  render() {
    return (
      <span>
        <PageHeader title="LDAP Settings">
          <span>This page is the only resource you need to set up the Graylog LDAP integration. You can test the connection to your LDAP server and even try to log in with an LDAP account of your choice right away.</span>

          <span>Read more about LDAP configuration in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES} text="documentation"/>.</span>

          <span>
            <IfPermitted permissions="LDAPGROUPS_EDIT">
              <LinkContainer to={Routes.SYSTEM.LDAP.GROUPS}>
                <Button bsStyle="info">LDAP Group Mapping</Button>
              </LinkContainer>
            </IfPermitted>
            &nbsp;
            <IfPermitted permissions="USERS_LIST">
              <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.USERS.LIST}>
                <Button bsStyle="info">Manage users</Button>
              </LinkContainer>
            </IfPermitted>
          </span>
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
