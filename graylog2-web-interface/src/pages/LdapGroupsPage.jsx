import React from 'react';
import Reflux from 'reflux';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, Row, Col } from 'react-bootstrap';

import { IfPermitted, PageHeader, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import LdapGroupsComponent from 'components/ldap/LdapGroupsComponent';

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const LdapStore = StoreProvider.getStore('Ldap');

const LdapGroupsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore), Reflux.connect(LdapStore)],

  _areGroupsEnabled(ldapSettings) {
    return ldapSettings.group_search_base && ldapSettings.group_search_pattern;
  },

  _getContent() {
    if (!this.state.ldapSettings) {
      return <Spinner/>;
    }
    if (!this.state.ldapSettings.enabled) {
      return (
        <p>
          LDAP is disabled, cannot edit LDAP group mapping. Please enable LDAP integration in the{' '}
          <LinkContainer to={Routes.SYSTEM.LDAP.SETTINGS}><a>LDAP settings</a></LinkContainer>.
        </p>
      );
    }

    if (!this._areGroupsEnabled(this.state.ldapSettings)) {
      return (
        <p>
          Required LDAP configuration is not set, please check the{' '}
          <LinkContainer to={Routes.SYSTEM.LDAP.SETTINGS}><a>LDAP configuration settings</a></LinkContainer>{' '}
          to enable group mapping.
        </p>
      );
    }

    return <LdapGroupsComponent/>;
  },

  render() {
    return (
      <span>
        <PageHeader title="LDAP Group Mapping">
          <span>Map LDAP groups to Graylog roles</span>

          <span>
            LDAP groups with no defined mapping will use the defaults set in your{' '}
            <LinkContainer to={Routes.SYSTEM.LDAP.SETTINGS}><a>LDAP settings</a></LinkContainer>.{' '}
            Read more about it in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES} text="documentation"/>.
          </span>

          <span>
            <IfPermitted permissions="ldap:edit">
              <LinkContainer to={Routes.SYSTEM.LDAP.SETTINGS}>
                <Button bsStyle="info">Configure LDAP</Button>
              </LinkContainer>
            </IfPermitted>
            &nbsp;
            <IfPermitted permissions="users:list">
              <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.USERS.LIST}>
                <Button bsStyle="info">Manage users</Button>
              </LinkContainer>
            </IfPermitted>
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={8}>
            {this._getContent()}
          </Col>
        </Row>
      </span>
    );
  },
});

export default LdapGroupsPage;
