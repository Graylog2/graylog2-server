import React from 'react';

import { Row, Col, Input, Button} from 'react-bootstrap';

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

  _bindValue(ev) {
    this._setSetting(ev.target.name, ev.target.value);
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
  helpTextsAD: {
    SYSTEM_USERNAME: (
      <span>The username for the initial connection to the Active Directory server, e.g. <code>ldapbind@@some.domain</code>.<br/>
      This needs to match the <code>userPrincipalName</code> of that user.</span>),
    SYSTEM_PASSWORD: ('The password for the initial connection to the Active Directory server.'),
  },
  helpTextsLDAP: {
    SYSTEM_USERNAME: (
      <span>The username for the initial connection to the LDAP server, e.g. <code>uid=admin,ou=system</code>, this might be optional depending on your LDAP server.</span>),
    SYSTEM_PASSWORD: ('The password for the initial connection to the LDAP server.'),
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    const isAD = this.state.ldapSettings.active_directory;
    const disabled = !this.state.ldapSettings.enabled;
    const help = isAD ? this.helpTextsAD : this.helpTextsLDAP;

    return (<Row>
      <Col lg={8}>
        <form id="ldap-settings-form" className="form-horizontal">
          <Input type="checkbox" label="Enable LDAP"
                 help="User accounts will be taken from LDAP/Active Directory, the administrator account will still be available."
                 wrapperClassName="col-sm-offset-3 col-sm-9"
                 name="enabled"
                 onChange={this._bindChecked}/>

          <fieldset>
            <legend className="col-sm-12">1. Server configuration</legend>
            <div className="form-group">
              <label className="col-sm-3 control-label">Server Type</label>
              <Col sm={9}>
                <label className="radio-inline">
                  <input type="radio" name="active_directory"
                         checked={!isAD} disabled={disabled}
                         onChange={(ev) => this._bindChecked(ev, false)}/>
                  LDAP
                </label>
                <label className="radio-inline">
                  <input type="radio" name="active_directory"
                         checked={isAD} disabled={disabled}
                         onChange={(ev) => this._bindChecked(ev, true)}/>
                  Active Directory
                </label>
              </Col>
            </div>

            <div className="form-group">
              <label className="col-sm-3 control-label">Server Address</label>
              <Col sm={9}>
                <div className="input-group">
                  <span className="input-group-addon">{this._uriScheme()}</span>
                  <input type="text" className="form-control" id="ldap-uri-host" value={this._uriHost()}
                         placeholder="Hostname" required onChange={(ev) => this._setUriHost(ev.target.value)}
                         disabled={disabled}/>
                  <span className="input-group-addon input-group-separator">:</span>
                  <input type="number" className="form-control" id="ldap-uri-port" value={this._uriPort()} min="1"
                         max="65535" placeholder="Port"
                         required style={{width: 120}} onChange={(ev) => this._setUriPort(ev.target.value)}
                         disabled={disabled}/>
                </div>
                <label className="checkbox-inline">
                  <input type="checkbox" name="ssl" checked={this.state.ldapSettings.ssl} onChange={this._updateSsl}
                         disabled={disabled}/> SSL
                </label>
                <label className="checkbox-inline">
                  <input type="checkbox" name="use_start_tls" value="true" id="ldap-uri-starttls"
                         checked={this.state.ldapSettings.use_start_tls} onChange={this._bindChecked}
                         disabled={disabled}/> StartTLS
                </label>
                <label className="checkbox-inline">
                  <input type="checkbox" name="trust_all_certificates" value="true" id="trust-all-certificates"
                         checked={this.state.ldapSettings.trust_all_certificates} onChange={this._bindChecked}
                         disabled={disabled}/> Allow self-signed certificates
                </label>
              </Col>
            </div>

            <div className="form-group">
              <label className="col-sm-3 control-label" htmlFor="system_username">System Username</label>
              <div className="col-sm-9">
                <input type="text" id="system_username" className="form-control"
                       value={this.state.ldapSettings.system_username} name="system_username"
                       placeholder="System User DN" onChange={this._bindValue} disabled={disabled}/>
                <span className="help-block">{help.SYSTEM_USERNAME}</span>
              </div>
            </div>
            <div className="form-group">
              <label className="col-sm-3 control-label" htmlFor="system_password">System Password</label>
              <div className="col-sm-9">
                <input type="password" id="system_password" className="form-control"
                       value={this.state.ldapSettings.system_password} name="system_password"
                       placeholder="System Password" onChange={this._bindValue} disabled={disabled}/>
                <span className="help-block">{help.SYSTEM_PASSWORD}</span>
              </div>
            </div>
          </fieldset>
          <fieldset>
              <legend className="col-sm-12">2. Connection Test</legend>
              <div className="form-group">
                <div className="col-sm-offset-3 col-sm-9">
                  <button type="button" id="ldap-test-connection" className="btn btn-warning" disabled={disabled || this.state.ldapSettings.ldap_uri.hostname() === ''}>
                    Test Server Connection
                  </button>
                  <span className="help-block">Performs a background connection check with the address and credentials above.</span>
                  <div className="alert alert-danger" id="ldap-connectionfailure-reason" style={{display: 'none'}}></div>
                </div>
              </div>

          </fieldset>

          <fieldset>
            <legend className="col-sm-12">3. User mapping</legend>

          </fieldset>

          <fieldset>
            <legend className="col-sm-12">4. Group Mapping <small>(optional)</small></legend>

          </fieldset>

          <fieldset>
            <legend className="col-sm-12">5. Login test</legend>

          </fieldset>

          <div className="form-group">
            <div className="col-sm-offset-3 col-sm-9">
              <Button bsStyle="success">Save LDAP settings</Button>
            </div>
          </div>
        </form>
      </Col>
    </Row>);
  },
});

module.exports = LdapComponent;
