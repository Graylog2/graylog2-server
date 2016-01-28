import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';
import URI from 'urijs';

import DocsHelper from 'util/DocsHelper';

import CurrentUserStore from 'stores/users/CurrentUserStore';
import LdapStore from 'stores/users/LdapStore';

import { PageHeader, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import LdapComponent from 'components/users/LdapComponent';

const LdapPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore), Reflux.listenTo(LdapStore, '_onLdapSettingsChange')],
  getInitialState() {
    return {
      ldapSettings: undefined,
    };
  },
  _onLdapSettingsChange(state) {
    if (!state.ldapSettings) {
      return;
    }

    // Clone settings object, so we don't the store reference
    const settings = JSON.parse(JSON.stringify(state.ldapSettings));
    settings.ldap_uri = new URI(settings.ldap_uri);

    this.setState({ldapSettings: settings});
  },
  _isLoading() {
    return !this.state.ldapSettings;
  },
  render() {
    let content;

    if (this._isLoading()) {
      content = <Spinner/>;
    } else {
      content = <LdapComponent ldapSettings={this.state.ldapSettings} />;
    }

    return (
      <span>
        <PageHeader title="LDAP Settings" titleSize={8} buttonSize={4} buttonStyle={{textAlign: 'right', marginTop: '10px'}}>
          <span>This page is the only resource you need to set up the Graylog LDAP integration. You can test the connection to your LDAP server and even try to log in with an LDAP account of your choice right away.</span>

          <span>Read more about LDAP configuration in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES} text="documentation"/>.</span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            {content}
          </Col>
        </Row>
      </span>
    );
  },
});

export default LdapPage;
