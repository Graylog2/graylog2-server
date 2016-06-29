import React from 'react';
import Reflux from 'reflux';
import { Alert, Nav, NavItem, Row, Col } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';
import { Spinner } from 'components/common';
import { PluginStore } from 'graylog-web-plugin/plugin';

import PermissionsMixin from 'util/PermissionsMixin';
import AuthProvidersConfig from './AuthProvidersConfig';

import ActionsProvider from 'injection/ActionsProvider';
const AuthenticationActions = ActionsProvider.getActions('Authentication');

import StoreProvider from 'injection/StoreProvider';
const AuthenticationStore = StoreProvider.getStore('Authentication');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');


const AuthenticationComponent = React.createClass({

  propTypes: {
    location: React.PropTypes.object.isRequired,
    params: React.PropTypes.object.isRequired,
    children: React.PropTypes.element,
  },

  mixins: [Reflux.connect(AuthenticationStore), Reflux.connect(CurrentUserStore), PermissionsMixin],

  getInitialState() {
    return {
      activeTab: 'users',
    };
  },

  componentDidMount() {
    this.style.use();
    AuthenticationActions.load();

    PluginStore.exports('authenticatorConfigurations').forEach((authConfig) => {
      this.authenticatorConfigurations[authConfig.name] = authConfig;
      // TODO load per authenticator config
    });
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  // contains the 'authname' -> plugin descriptor
  authenticatorConfigurations: {},

  style: require('!style/useable!css!./AuthenticationComponent.css'),

  _pluginPane() {
    const name = this.props.params.name;
    const auth = this.authenticatorConfigurations[name];

    if (auth) {
      return React.createElement(auth.component, {
        key: `auth-configuration-${name}`,
      });
    } else {
      return (<Alert bsStyle="danger">Plugin component missing for authenticator <code>{name}</code>, this is an error.</Alert>);
    }
  },

  _onUpdateProviders(config) {
    return AuthenticationActions.update('providers', config);
  },

  _handleTabChange(key) {
    this.setState({ activeTab: key });
  },

  _contentComponent() {
    if (!this.state.authenticators) {
      return <Spinner />;
    }
    if (this.props.params.name === undefined) {
      return (<AuthProvidersConfig config={this.state.authenticators}
                                  descriptors={this.authenticatorConfigurations}
                                  updateConfig={this._onUpdateProviders} />);
    }
    return this._pluginPane();
  },

  render() {
    let authenticators = [<NavItem key={"loading"} disabled title="Loading...">Loading...</NavItem>];
    const auths = this.state.authenticators;
    if (auths) {
      authenticators = auths.realm_order.map((name, idx) => {
        const auth = this.authenticatorConfigurations[name];
        const title = (auth || { displayName: name }).displayName;
        const numberedTitle = `${idx + 1}. ${title}`;
        return (<LinkContainer key={`container-${name}`} to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.provider(name)}>
          <NavItem key={name} eventKey={name} title={numberedTitle}>{numberedTitle}</NavItem>
        </LinkContainer>);
      });

      authenticators.unshift(
        <LinkContainer key="container-settings" to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CONFIG}>
          <NavItem key="settings" eventKey="config" title="Configure Providers">Configure providers</NavItem>
        </LinkContainer>
      );
    }

    // add submenu items based on permissions
    if (this.isPermitted(this.state.currentUser.permissions, ['ROLES_READ'])) {
      authenticators.unshift(
        <LinkContainer key="roles" to={Routes.SYSTEM.AUTHENTICATION.ROLES}>
          <NavItem eventKey="roles" title="Roles">Roles</NavItem>
        </LinkContainer>
      );
    }
    if (this.isPermitted(this.state.currentUser.permissions, ['ROLES_READ'])) {
      authenticators.unshift(
        <LinkContainer key="users" to={Routes.SYSTEM.AUTHENTICATION.USERS.LIST}>
          <NavItem eventKey="users" title="Users">Users</NavItem>
        </LinkContainer>
      );
    }
    const subnavigation = (
      <Nav activeKey={this.state.activeTab} onSelect={this._handleTabChange} stacked bsStyle="pills">
        {authenticators}
      </Nav>
    );

    let contentComponent = React.Children.count(this.props.children) === 1 ? React.Children.only(this.props.children) : this._contentComponent();

    return (<Row>
      <Col md={2} className="subnavigation">{subnavigation}</Col>
      <Col md={10} className="contentpane">{contentComponent}</Col>
    </Row>);
  },
});

export default AuthenticationComponent;
