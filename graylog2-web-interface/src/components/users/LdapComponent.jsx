import React from 'react';

import { Row, Col, Input } from 'react-bootstrap';

import Spinner from '../common/Spinner';

import RolesStore from 'stores/users/RolesStore';
import LdapStore from 'stores/users/LdapStore';
import LdapGroupsStore from 'stores/users/LdapGroupsStore';
import URI from 'urijs';

const LdapComponent = React.createClass({
  getInitialState() {
    return {
      ldapSettings: null,
    };
  },

  componentDidMount() {
    LdapStore.loadSettings().done(settings => {
      settings.ldap_uri = new URI(settings.ldap_uri);
      this.setState({ldapSettings: settings});
    });
  },

  _isLoading() {
    return !this.state.ldapSettings;
  },

  _bindChecked(ev, value) {
    this._setSetting(ev.target.name, typeof value === 'undefined' ? ev.target.checked : value);
  },

  _updateSsl(ev) {
    this._setUriScheme(ev.target.checked ? 'ldaps' : 'ldap');
  },

  _setSetting(attribute, value) {
    const settings = this.state.ldapSettings;
    settings[attribute] = value;
    this.setState({ldapSettings: settings});
  },

  _setUriScheme(scheme) {
    const ldapUri = this.state.ldapSettings.ldap_uri;
    ldapUri.scheme(scheme);
    this._setSetting({ldap_uri: ldapUri});
  },

  _uriScheme() {
    return this.state.ldapSettings.ldap_uri.scheme() + '://';
  },

  _setUriHost(host) {
    const ldapUri = this.state.ldapSettings.ldap_uri;
    ldapUri.host(host);
    this._setSetting({ldap_uri: ldapUri});
  },

  _uriHost() {
    return this.state.ldapSettings.ldap_uri.hostname();
  },

  _setUriPort(port) {
    const ldapUri = this.state.ldapSettings.ldap_uri;
    ldapUri.port(port);
    this._setSetting({ldap_uri: ldapUri});
  },

  _uriPort() {
    return this.state.ldapSettings.ldap_uri.port();
  },

  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }
    return (<Row>
      <Col lg={8}>
        <form id="ldap-settings-form" className="form-horizontal">
          <Input type="checkbox" label="Enable LDAP"
                 help="User accounts will be taken from LDAP/Active Directory, the administrator account will still be available."
                 wrapperClassName="col-sm-offset-3 col-sm-9"
                 name="enabled"
                 onChange={this._bindChecked}/>

          <fieldset>
            <legend className="col-sm-offset-3 col-sm-9">Server configuration</legend>
            <div className="form-group">
              <label className="col-sm-3 control-label">Server Type</label>
              <Col sm={9}>
                <label className="radio-inline">
                  <input type="radio" name="active_directory"
                         checked={!this.state.ldapSettings.active_directory}
                         onChange={(ev) => this._bindChecked(ev, false)}/>
                  <span>LDAP</span>
                </label>
                <label className="radio-inline">
                  <input type="radio" name="active_directory"
                         checked={this.state.ldapSettings.active_directory}
                         onChange={(ev) => this._bindChecked(ev, true)}/>
                  <span>Active Directory</span>
                </label>
              </Col>
            </div>

            <div className="form-group">
              <label className="col-sm-3 control-label">Server Address</label>
              <Col sm={9}>
                <div className="input-group">
                  <span className="input-group-addon">{this._uriScheme()}</span>
                  <input type="text" className="form-control" id="ldap-uri-host" value={this._uriHost()} placeholder="Hostname" required onChange={(ev) => this._setUriHost(ev.target.value)}/>
                  <span className="input-group-addon input-group-separator">:</span>
                  <input type="number" className="form-control" id="ldap-uri-port" value={this._uriPort()} min="1" max="65535" placeholder="Port"
                         required style={{width: 120}} onChange={(ev) => this._setUriPort(ev.target.value)}/>
                </div>
                <label className="checkbox-inline">
                  <input type="checkbox" name="ssl" checked={this.state.ldapSettings.ssl} onChange={this._updateSsl}/> SSL
                </label>
                <label className="checkbox-inline">
                  <input type="checkbox" name="use_start_tls" value="true" id="ldap-uri-starttls"
                         checked={this.state.ldapSettings.use_start_tls} onChange={this._bindChecked}/> StartTLS
                </label>
                <label className="checkbox-inline">
                  <input type="checkbox" name="trust_all_certificates" value="true" id="trust-all-certificates"
                         checked={this.state.ldapSettings.trust_all_certificates} onChange={this._bindChecked}/> Allow self-signed certificates
                </label>
              </Col>
            </div>

          </fieldset>


        </form>
      </Col>
    </Row>);
  },
});

module.exports = LdapComponent;
