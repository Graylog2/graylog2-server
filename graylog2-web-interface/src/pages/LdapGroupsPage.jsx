import React from 'react';
import Reflux from 'reflux';
import { LinkContainer } from 'react-router-bootstrap';
import { Row, Col } from 'react-bootstrap';

import { PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

import CurrentUserStore from 'stores/users/CurrentUserStore';

const LdapGroupsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    return (
      <span>
        <PageHeader title="LDAP Group Mapping" titleSize={8} buttonSize={4} buttonStyle={{textAlign: 'right', marginTop: '10px'}}>
          <span>Map LDAP groups to Graylog roles</span>

          <span>
            LDAP groups with no defined mapping will use the defaults set in your{' '}
            <LinkContainer to={Routes.SYSTEM.LDAP.SETTINGS}><a>LDAP settings</a></LinkContainer>.{' '}
            Read more about it in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES} text="documentation"/>.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <div>This is not the page you are looking for.</div>
          </Col>
        </Row>
      </span>
    );
  },
});

export default LdapGroupsPage;
