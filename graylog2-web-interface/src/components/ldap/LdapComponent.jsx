import PropTypes from 'prop-types';
import React from 'react';
import URI from 'urijs';
import naturalSort from 'javascript-natural-sort';
import { cloneDeep } from 'lodash';

import { Row, Col, Panel, FormGroup, ControlLabel, Button } from 'components/graylog';
import { Input, InputWrapper } from 'components/bootstrap';

import { MultiSelect, Select, Spinner } from 'components/common';

import CombinedProvider from 'injection/CombinedProvider';
import TestLdapConnection from './TestLdapConnection';
import TestLdapLogin from './TestLdapLogin';
import LdapComponentStyle from './LdapComponent.css';


const { RolesStore } = CombinedProvider.get('Roles');
const { LdapActions } = CombinedProvider.get('Ldap');

const HelperText = {
  activeDirectory: {
    SYSTEM_USERNAME: (
      <span>
        The username for the initial connection to the Active Directory server, e.g. <code>ldapbind@some.domain</code>.<br />
        This needs to match the <code>userPrincipalName</code> of that user.
      </span>
    ),
    SYSTEM_PASSWORD: ('The password for the initial connection to the Active Directory server.'),
    SEARCH_BASE: (
      <span>
        The base tree to limit the Active Directory search query to, e.g. <code>cn=users,dc=example,dc=com</code>.
      </span>
    ),
    SEARCH_PATTERN: (
      <span>
        For example <code className="text-nowrap">{'(&(objectClass=user)(sAMAccountName={0}))'}</code>.{' '}
        The string <code>{'{0}'}</code> will be replaced by the entered username.
      </span>
    ),
    DISPLAY_NAME: (
      <span>
        Which Active Directory attribute to use for the full name of the user in Graylog, e.g. <code>displayName</code>.<br />
        Try to load a test user using the form below, if you are unsure which attribute to use.
      </span>
    ),
    GROUP_SEARCH_BASE: (
      <span>
        The base tree to limit the Active Directory group search query to, e.g. <code>cn=users,dc=example,dc=com</code>.
      </span>
    ),
    GROUP_PATTERN: (
      <span>
        The search pattern used to find groups in Active Directory for mapping to Graylog roles, e.g.{' '}
        <code className="text-nowrap">(objectClass=group)</code> or{' '}
        <code className="text-nowrap">(&amp;(objectClass=group)(cn=graylog*))</code>.
      </span>
    ),
    GROUP_ID: (
      <span>Which Active Directory attribute to use for the full name of the group, usually <code>cn</code>.</span>
    ),
    defaultGroup: onClickHandler => (
      <span>
        The default Graylog role determines whether a user created via Active Directory can access the entire system, or has limited access.<br />
        You can assign additional permissions by{' '}
        <Button bsStyle="link" bsSize="xs" className="btn-text" onClick={onClickHandler}>mapping Active Directory groups to Graylog roles</Button>,{' '}
        or you can assign additional Graylog roles to Active Directory users below.
      </span>
    ),
    ADDITIONAL_GROUPS: (
      'Choose the additional roles each Active Directory user will have by default, leave it empty if you want to map Active Directory groups to Graylog roles.'
    ),
  },

  ldap: {
    SYSTEM_USERNAME: (
      <span>
        The username for the initial connection to the LDAP server, e.g.{' '}
        <code className="text-nowrap">uid=admin,ou=system</code>, this might be optional depending on your LDAP server.
      </span>
    ),
    SYSTEM_PASSWORD: ('The password for the initial connection to the LDAP server.'),
    SEARCH_BASE: (
      <span>
        The base tree to limit the LDAP search query to, e.g.{' '}
        <code className="text-nowrap">cn=users,dc=example,dc=com </code>.
      </span>
    ),
    SEARCH_PATTERN: (
      <span>
        For example <code className="text-nowrap">{'(&(objectClass=inetOrgPerson)(uid={0}))'}</code>.{' '}
        The string <code>{'{0}'}</code> will be replaced by the entered username.
      </span>
    ),
    DISPLAY_NAME: (
      <span>
        Which LDAP attribute to use for the full name of the user in Graylog, e.g. <code>cn</code>.<br />
        Try to load a test user using the form below, if you are unsure which attribute to use.
      </span>
    ),
    GROUP_SEARCH_BASE: (
      <span>
        The base tree to limit the LDAP group search query to, e.g. <code>cn=users,dc=example,dc=com</code>.
      </span>
    ),
    GROUP_PATTERN: (
      <span>
        The search pattern used to find groups in LDAP for mapping to Graylog roles, e.g.{' '}
        <code>(objectClass=groupOfNames)</code> or{' '}
        <code className="text-nowrap">(&amp;(objectClass=groupOfNames)(cn=graylog*))</code>.
      </span>
    ),
    GROUP_ID: (
      <span>Which LDAP attribute to use for the full name of the group, usually <code>cn</code>.</span>
    ),
    defaultGroup: onClickHandler => (
      <span>
        The default Graylog role determines whether a user created via LDAP can access the entire system, or has limited access.<br />
        You can assign additional permissions by{' '}
        <Button bsStyle="link" bsSize="xs" className="btn-text" onClick={onClickHandler}>mapping LDAP groups to Graylog roles</Button>,{' '}
        or you can assign additional Graylog roles to LDAP users below.
      </span>
    ),
    ADDITIONAL_GROUPS: (
      'Choose the additional roles each LDAP user will have by default, leave it empty if you want to map LDAP groups to Graylog roles.'
    ),
  },
};

class LdapComponent extends React.Component {
  static propTypes = {
    onCancel: PropTypes.func.isRequired,
    onShowGroups: PropTypes.func.isRequired,
  };

  state = {
    ldapSettings: undefined,
    ldapUri: undefined,
    roles: undefined,
    hidePasswordInput: true,
  };

  componentDidMount() {
    LdapActions.loadSettings().then(this._onLdapSettingsChange);
    RolesStore.loadRoles().then((roles) => {
      this.setState({ roles: this._formatAdditionalRoles(roles) });
    });
  }

  _formatAdditionalRoles = (roles) => {
    return roles
      .sort((r1, r2) => naturalSort(r1.name.toLowerCase(), r2.name.toLowerCase()))
      .map((r) => {
        return { label: r.name, value: r.name };
      });
  };

  _onLdapSettingsChange = (ldapSettings) => {
    const settings = cloneDeep(ldapSettings);
    const ldapUri = new URI(settings.ldap_uri);
    this.setState({ ldapSettings: settings, ldapUri: ldapUri, hidePasswordInput: settings.system_password_set });
  };

  _isLoading = () => {
    const { ldapSettings, roles } = this.state;
    return !ldapSettings || !roles;
  };

  _bindChecked = (ev, value) => {
    this._setSetting(ev.target.name, typeof value === 'undefined' ? ev.target.checked : value);
  };

  _bindValue = (ev) => {
    this._setSetting(ev.target.name, ev.target.value);
  };

  _updateSsl = (ev) => {
    this._setUriScheme(ev.target.checked ? 'ldaps' : 'ldap');
  };

  _setSetting = (attribute, value) => {
    const newState = {};

    let formattedValue = value;
    // Convert URI object into string to store it in the state
    if (attribute === 'ldap_uri' && typeof value === 'object') {
      newState.ldapUri = value;
      formattedValue = value.toString();
    }

    // Clone state to not modify it directly
    const { ldapSettings } = this.state;
    const settings = cloneDeep(ldapSettings);
    settings[attribute] = formattedValue;
    newState.ldapSettings = settings;
    newState.serverConnectionStatus = {};
    this.setState(newState);
  };

  _setUriScheme = (scheme) => {
    const { ldapUri } = this.state;
    const nextLdapUri = ldapUri.clone();
    nextLdapUri.scheme(scheme);
    this._setSetting('ldap_uri', nextLdapUri);
  };

  _uriScheme = () => {
    const { ldapUri } = this.state;
    return `${ldapUri.scheme()}://`;
  };

  _setUriHost = (host) => {
    const { ldapUri } = this.state;
    const nextLdapUri = ldapUri.clone();
    nextLdapUri.hostname(host);
    this._setSetting('ldap_uri', nextLdapUri);
  };

  _uriHost = () => {
    const { ldapUri } = this.state;
    return ldapUri.hostname();
  };

  _setUriPort = (port) => {
    const { ldapUri } = this.state;
    const nextLdapUri = ldapUri.clone();
    nextLdapUri.port(port);
    this._setSetting('ldap_uri', nextLdapUri);
  };

  _uriPort = () => {
    const { ldapUri } = this.state;
    return ldapUri.port();
  };

  _setAdditionalDefaultGroups = (rolesString) => {
    // only keep non-empty entries
    const roles = rolesString.split(',').filter(v => v !== '');
    this._setSetting('additional_default_groups', roles);
  };

  _saveSettings = (event) => {
    event.preventDefault();
    const { ldapSettings } = this.state;
    LdapActions.update(ldapSettings);
  };

  _onShowGroups = (event) => {
    event.preventDefault();
    const { onShowGroups } = this.props;
    onShowGroups();
  };

  _showPasswordInput = () => {
    this.setState({ hidePasswordInput: false });
  };

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { hidePasswordInput, ldapSettings, ldapUri, roles: rolesOptions } = this.state;
    const { onCancel } = this.props;

    const isAD = ldapSettings.active_directory;
    const disabled = !ldapSettings.enabled;
    const help = isAD ? HelperText.activeDirectory : HelperText.ldap;

    const ldapPasswordInput = hidePasswordInput
      ? (
        <FormGroup controlId="system_password">
          <ControlLabel className="col-sm-3">System Password</ControlLabel>
          <InputWrapper className="col-sm-9">
            <span className={LdapComponentStyle.passwordSet}>Password is set</span>
            <Button onClick={this._showPasswordInput}>Reset Password</Button>
          </InputWrapper>
        </FormGroup>
      )
      : (
        <Input type="password"
               id="system_password"
               name="system_password"
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9"
               placeholder="System Password"
               label="System Password"
               value={ldapSettings.system_password}
               help={help.SYSTEM_PASSWORD}
               onChange={this._bindValue}
               disabled={disabled} />
      );

    const additionalDefaultGroups = ldapSettings.additional_default_groups;
    const additionalDefaultGroupsString = Array.isArray(additionalDefaultGroups)
      ? additionalDefaultGroups.join(',')
      : additionalDefaultGroups;

    return (
      <Row>
        <Col lg={8}>
          <form id="ldap-settings-form" className="form-horizontal" onSubmit={this._saveSettings}>
            <Input id="enable-ldap-checkbox"
                   type="checkbox"
                   label="Enable LDAP"
                   help="User accounts will be taken from LDAP/Active Directory, the administrator account will still be available."
                   wrapperClassName="col-sm-offset-3 col-sm-9"
                   name="enabled"
                   checked={ldapSettings.enabled}
                   onChange={this._bindChecked} />

            <fieldset>
              <Row className="row-sm">
                <Col sm={12}>
                  <legend>1. Server configuration</legend>
                </Col>
              </Row>
              <Input id="active_directory"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     label="Server Type">
                <>
                  <label className="radio-inline" htmlFor="ldap-checkbox">
                    <input type="radio"
                           name="active_directory"
                           id="ldap-checkbox"
                           checked={!isAD}
                           disabled={disabled}
                           onChange={ev => this._bindChecked(ev, false)} />
                  LDAP
                  </label>
                  <label className="radio-inline" htmlFor="ad-checkbox">
                    <input type="radio"
                           name="active_directory"
                           id="ad-checkbox"
                           checked={isAD}
                           disabled={disabled}
                           onChange={ev => this._bindChecked(ev, true)} />
                  Active Directory
                  </label>
                </>
              </Input>

              <Input id="ldap-uri-host"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     label="Server Address">
                <>
                  <div className="input-group">
                    <span className="input-group-addon">{this._uriScheme()}</span>
                    <input type="text"
                           className="form-control"
                           id="ldap-uri-host"
                           value={this._uriHost()}
                           placeholder="Hostname"
                           required
                           onChange={ev => this._setUriHost(ev.target.value)}
                           disabled={disabled} />
                    <span className="input-group-addon input-group-separator">:</span>
                    <input type="number"
                           className="form-control"
                           id="ldap-uri-port"
                           value={this._uriPort()}
                           min="1"
                           max="65535"
                           placeholder="Port"
                           required
                           style={{ width: 120 }}
                           onChange={ev => this._setUriPort(ev.target.value)}
                           disabled={disabled} />
                  </div>
                  <label className="checkbox-inline" htmlFor="ldap-ssl-checkbox">
                    <input type="checkbox"
                           name="ssl"
                           id="ldap-ssl-checkbox"
                           checked={ldapUri.scheme() === 'ldaps'}
                           onChange={this._updateSsl}
                           disabled={disabled} /> SSL
                  </label>
                  <label className="checkbox-inline" htmlFor="ldap-uri-starttls">
                    <input type="checkbox"
                           name="use_start_tls"
                           value="true"
                           id="ldap-uri-starttls"
                           checked={ldapSettings.use_start_tls}
                           onChange={this._bindChecked}
                           disabled={disabled} /> StartTLS
                  </label>
                  <label className="checkbox-inline" htmlFor="trust-all-certificates">
                    <input type="checkbox"
                           name="trust_all_certificates"
                           value="true"
                           id="trust-all-certificates"
                           checked={ldapSettings.trust_all_certificates}
                           onChange={this._bindChecked}
                           disabled={disabled} /> Allow self-signed certificates
                  </label>
                </>
              </Input>

              <Input type="text"
                     id="system_username"
                     name="system_username"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     placeholder="System User DN"
                     label="System Username"
                     value={ldapSettings.system_username}
                     help={help.SYSTEM_USERNAME}
                     onChange={this._bindValue}
                     disabled={disabled} />

              {ldapPasswordInput}
            </fieldset>

            <fieldset>
              <Row className="row-sm">
                <Col sm={12}>
                  <legend>2. Connection Test</legend>
                </Col>
              </Row>
              <TestLdapConnection ldapSettings={ldapSettings} ldapUri={ldapUri} disabled={disabled} />
            </fieldset>

            <fieldset>
              <Row className="row-sm">
                <Col sm={12}>
                  <legend>3. User mapping</legend>
                </Col>
              </Row>
              <Input type="text"
                     id="search_base"
                     name="search_base"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     placeholder="Search Base"
                     label="Search Base DN"
                     value={ldapSettings.search_base}
                     help={help.SEARCH_BASE}
                     onChange={this._bindValue}
                     disabled={disabled}
                     required />

              <Input type="text"
                     id="search_pattern"
                     name="search_pattern"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     placeholder="Search Pattern"
                     label="User Search Pattern"
                     value={ldapSettings.search_pattern}
                     help={help.SEARCH_PATTERN}
                     onChange={this._bindValue}
                     disabled={disabled}
                     required />

              <Input type="text"
                     id="display_name_attribute"
                     name="display_name_attribute"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     placeholder="Display Name Attribute"
                     label="Display Name attribute"
                     value={ldapSettings.display_name_attribute}
                     help={help.DISPLAY_NAME}
                     onChange={this._bindValue}
                     disabled={disabled}
                     required />
            </fieldset>

            <fieldset>
              <Row className="row-sm">
                <Col sm={12}>
                  <legend>4. Group Mapping <small>(optional)</small></legend>
                </Col>
              </Row>
              <Input type="text"
                     id="group_search_base"
                     name="group_search_base"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     placeholder="Group Search Base"
                     label="Group Search Base DN"
                     value={ldapSettings.group_search_base}
                     help={help.GROUP_SEARCH_BASE}
                     onChange={this._bindValue}
                     disabled={disabled} />

              <Input type="text"
                     id="group_search_pattern"
                     name="group_search_pattern"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     placeholder="Group Search Pattern"
                     label="Group Search Pattern"
                     value={ldapSettings.group_search_pattern}
                     help={help.GROUP_PATTERN}
                     onChange={this._bindValue}
                     disabled={disabled} />

              <Input type="text"
                     id="group_id_attribute"
                     name="group_id_attribute"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     placeholder="Group Id Attribute"
                     label="Group Name Attribute"
                     value={ldapSettings.group_id_attribute}
                     help={help.GROUP_ID}
                     onChange={this._bindValue}
                     disabled={disabled} />

              <Input id="default_group"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     label="Default User Role"
                     help={help.defaultGroup(this._onShowGroups)}>
                <Select options={rolesOptions}
                        disabled={disabled}
                        value={ldapSettings.default_group}
                        onChange={role => this._setSetting('default_group', role)}
                        placeholder="Choose a default role" />
              </Input>

              <Row>
                <Col sm={9} smOffset={3}>
                  <Panel bsStyle="info">
                    Changing the static role assignment will only affect to new users created via LDAP/Active Directory!<br />
                    Existing user accounts will be updated on their next login, or if you edit their roles manually.
                  </Panel>
                </Col>
              </Row>

              <Input id="additional_default_groups"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     label="Additional Default Roles"
                     help={help.ADDITIONAL_GROUPS}>
                <MultiSelect options={rolesOptions}
                             disabled={disabled}
                             value={additionalDefaultGroupsString}
                             onChange={roles => this._setAdditionalDefaultGroups(roles)}
                             placeholder="Choose additional roles..." />
              </Input>

              <Row>
                <Col sm={9} smOffset={3}>
                  <Panel bsStyle="info">
                    Changing the static role assignment will only affect to new users created via LDAP/Active Directory!<br />
                    Existing user accounts will be updated on their next login, or if you edit their roles manually.
                  </Panel>
                </Col>
              </Row>
            </fieldset>

            <fieldset>
              <Row className="row-sm">
                <Col sm={12}>
                  <legend>5. Login test</legend>
                </Col>
              </Row>
              <TestLdapLogin ldapSettings={ldapSettings} disabled={disabled} />
            </fieldset>

            <fieldset>
              <Row className="row-sm">
                <Col sm={12}>
                  <legend>6. Store settings</legend>
                </Col>
              </Row>
              <div className="form-group">
                <Col sm={9} smOffset={3}>
                  <Button type="submit" bsStyle="primary" className="save-button-margin">Save LDAP settings</Button>
                  <Button onClick={onCancel}>Cancel</Button>
                </Col>
              </div>
            </fieldset>
          </form>
        </Col>
      </Row>
    );
  }
}

export default LdapComponent;
