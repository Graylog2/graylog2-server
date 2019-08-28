import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { Button, FormGroup, HelpBlock } from 'react-bootstrap';
import { BootstrapModalForm } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import FormUtils from 'util/FormsUtils';
import Input from 'components/bootstrap/Input';
import StringUtils from 'util/StringUtils';
import lodash from 'lodash';

const EmailConfig = createReactClass({
  displayName: 'EmailConfig',

  propTypes: {
    config: PropTypes.shape({
      enabled: PropTypes.bool,
      hostname: PropTypes.string,
      port: PropTypes.number,
      use_auth: PropTypes.bool,
      use_tls: PropTypes.bool,
      use_ssl: PropTypes.bool,
      username: PropTypes.string,
      password: PropTypes.string,
      from_email: PropTypes.string,
      web_interface_uri: PropTypes.string,
    }),
    updateConfig: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      config: {
        enabled: false,
        hostname: null,
        port: 25,
        use_auth: false,
        use_tls: true,
        use_ssl: false,
        username: null,
        password: null,
        from_email: null,
        web_interface_uri: null,
      },
    };
  },

  getInitialState() {
    const { config } = this.props;
    return {
      config: config,
    };
  },

  componentWillReceiveProps(newProps) {
    this.setState({ config: newProps.config });
  },

  _openModal() {
    this.modal.open();
  },

  _closeModal() {
    this.modal.close();
  },

  _resetConfig() {
    this.setState(this.getInitialState());
  },

  _saveConfig() {
    const { updateConfig } = this.props;
    const { config } = this.state;
    updateConfig(config).then(() => {
      this._closeModal();
    });
  },

  _propagateChanges(key, value) {
    const { config } = this.state;
    const nextConfig = lodash.cloneDeep(config);
    nextConfig[key] = value;
    this.setState({ config: nextConfig });
  },

  _onEnabledUpdate(event) {
    const value = FormUtils.getValueFromInput(event.target);
    this._propagateChanges('enabled', value);
  },

  _onHostnameUpdate(event) {
    const value = FormUtils.getValueFromInput(event.target);
    this._propagateChanges('hostname', value);
  },

  _onPortUpdate(event) {
    const value = FormUtils.getValueFromInput(event.target);
    this._propagateChanges('port', value);
  },

  _onUseAuthUpdate(event) {
    const value = FormUtils.getValueFromInput(event.target);
    this._propagateChanges('use_auth', value);
  },

  _onUseTlsUpdate(event) {
    const value = FormUtils.getValueFromInput(event.target);
    this._propagateChanges('use_tls', value);
  },

  _onUseSslUpdate(event) {
    const value = FormUtils.getValueFromInput(event.target);
    this._propagateChanges('use_ssl', value);
  },

  _onUsernameUpdate(event) {
    const value = FormUtils.getValueFromInput(event.target);
    this._propagateChanges('username', value);
  },

  _onPasswordUpdate(event) {
    const value = FormUtils.getValueFromInput(event.target);
    this._propagateChanges('password', value);
  },

  _onFromEmailUpdate(event) {
    const value = FormUtils.getValueFromInput(event.target);
    this._propagateChanges('from_email', value);
  },

  _onWebInterfaceUriUpdate(event) {
    const value = FormUtils.getValueFromInput(event.target);
    this._propagateChanges('web_interface_uri', value);
  },

  render() {
    const { config } = this.state;
    const enabled = config.enabled;
    const hostname = config.hostname;
    const port = config.port;
    const useAuth = config.use_auth;
    const useTls = config.use_tls;
    const useSsl = config.use_ssl;
    const username = config.username;
    const password = config.password;
    const fromEmail = config.from_email;
    const webInterfaceUri = config.web_interface_uri;
    return (
      <div>
        <h2>Email configuration</h2>

        <dl className="deflist">
          <dt>Enabled:</dt>
          <dd>{StringUtils.capitalizeFirstLetter(enabled.toString())}</dd>
          <dt>Hostname:</dt>
          <dd>{hostname ? hostname : '[not set]'}</dd>
          <dt>Port:</dt>
          <dd>{port}</dd>
          <dt>Use Auth:</dt>
          <dd>{StringUtils.capitalizeFirstLetter(useAuth.toString())}</dd>
          <dt>Use TLS:</dt>
          <dd>{StringUtils.capitalizeFirstLetter(useTls.toString())}</dd>
          <dt>Use SSL:</dt>
          <dd>{StringUtils.capitalizeFirstLetter(useSsl.toString())}</dd>
          <dt>Username:</dt>
          <dd>{username ? username : '[not set]'}</dd>
          <dt>Password:</dt>
          <dd>{password ? '***********' : '[not set]'}</dd>
          <dt>From email:</dt>
          <dd>{fromEmail ? fromEmail : '[not set]'}</dd>
          <dt>Web Interface URI:</dt>
          <dd>{webInterfaceUri ? webInterfaceUri : '[not set]'}</dd>
        </dl>

        <IfPermitted permissions="email:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
        </IfPermitted>

        <BootstrapModalForm ref={(modal) => { this.modal = modal; }}
                            title="Update Email Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">
          <fieldset>
            <Input id="enabled-field"
                   type="checkbox"
                   onChange={this._onEnabledUpdate}
                   label="Enable"
                   help="Enable Email configuration"
                   checked={enabled} />
            <Input id="hostname-field"
                   type="text"
                   onChange={this._onHostnameUpdate}
                   label="Hostname"
                   help="SMTP Server hostname or IP address"
                   value={hostname} />
            <Input id="port-field"
                   type="number"
                   onChange={this._onPortUpdate}
                   label="SMTP Port"
                   help="Port to connect to the SMTP server"
                   value={port}
                   min="0"
                   max="65535"
                   required />
            <Input id="use-auth-field"
                   type="checkbox"
                   onChange={this._onUseAuthUpdate}
                   label="Use Auth"
                   help="Use SMTP authentication"
                   checked={useAuth} />
            <Input id="use-tls-field"
                   type="checkbox"
                   onChange={this._onUseTlsUpdate}
                   label="Use TLS"
                   help="Use TLS"
                   checked={useTls} />
            <Input id="use-ssl-field"
                   type="checkbox"
                   onChange={this._onSslUpdate}
                   label="Use SSL"
                   help="Use SSL"
                   checked={useSsl} />
            <Input id="username-field"
                   type="text"
                   onChange={this._onUsernameUpdate}
                   label="Username"
                   help="SMTP Username (requires Use Auth to be enabled)"
                   value={username} />
            <Input id="password-field"
                   type="password"
                   onChange={this._onPasswordUpdate}
                   label="Password"
                   help="SMTP Password (requires Use Auth to be enabled)"
                   value={password} />
            <Input id="from-email-field"
                   type="text"
                   onChange={this._onFromEmailUpdate}
                   label="From email:"
                   help="Email address to use as default from address"
                   value={fromEmail} />
            <Input id="web-interface-uri-field"
                   type="text"
                   onChange={this._onWebInterfaceUriUpdate}
                   label="Web Interface URI:"
                   help="Web Interface URI"
                   value={webInterfaceUri} />
          </fieldset>
        </BootstrapModalForm>
      </div>
    )
  },
});

export default EmailConfig;
