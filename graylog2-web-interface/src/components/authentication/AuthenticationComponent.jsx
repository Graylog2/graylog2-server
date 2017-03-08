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

import AuthenticationComponentStyle from '!style!css!./AuthenticationComponent.css';

const AuthenticationComponent = React.createClass({

  propTypes: {
    location: React.PropTypes.object.isRequired,
    params: React.PropTypes.object.isRequired,
    history: React.PropTypes.object.isRequired,
    children: React.PropTypes.element,
  },

  mixins: [Reflux.connect(AuthenticationStore), Reflux.connect(CurrentUserStore), PermissionsMixin],

  componentDidMount() {
    AuthenticationActions.load();

    PluginStore.exports('authenticatorConfigurations').forEach((authConfig) => {
      this.authenticatorConfigurations[authConfig.name] = authConfig;
      // TODO load per authenticator config
    });
  },

  // contains the 'authname' -> plugin descriptor
  authenticatorConfigurations: {},

  _pluginPane() {
    const name = this.props.params.name;
    const auth = this.authenticatorConfigurations[name];

    if (auth) {
      return React.createElement(auth.component, {
        key: `auth-configuration-${name}`,
        history: this.props.history,
      });
    }
    return (<Alert bsStyle="danger">Plugin component missing for authenticator <code>{name}</code>, this is an error.</Alert>);
  },

  _onUpdateProviders(config) {
    return AuthenticationActions.update('providers', config);
  },

  _contentComponent() {
    if (!this.state.authenticators) {
      return <Spinner />;
    }
    if (this.props.params.name === undefined) {
      return (<AuthProvidersConfig config={this.state.authenticators}
                                   descriptors={this.authenticatorConfigurations}
                                   updateConfig={this._onUpdateProviders}
                                   history={this.props.history} />);
    }
    return this._pluginPane();
  },

  render() {
    let authenticators = [];
    const auths = this.state.authenticators;
    if (auths) {
      // only show the entries if the user is permitted to change them, makes no sense otherwise
      if (this.isPermitted(this.state.currentUser.permissions, ['authentication:edit'])) {
        authenticators = auths.realm_order.map((name, idx) => {
          const auth = this.authenticatorConfigurations[name];
          const title = (auth || { displayName: name }).displayName;
          const numberedTitle = `${idx + 1}. ${title}`;
          return (<LinkContainer key={`container-${name}`} to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.provider(name)}>
            <NavItem key={name} title={numberedTitle}>{numberedTitle}</NavItem>
          </LinkContainer>);
        });

        authenticators.unshift(
          <NavItem key="divider" disabled title="Provider Settings" className={AuthenticationComponentStyle.divider}>Provider Settings</NavItem>,
        );
        authenticators.unshift(
          <LinkContainer key="container-settings" to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CONFIG}>
            <NavItem key="settings" title="Configure Provider Order">Configure Provider Order</NavItem>
          </LinkContainer>,
        );
      }
    } else {
      authenticators = [<NavItem key={'loading'} disabled title="Loading...">Loading...</NavItem>];
    }

    // add submenu items based on permissions
    if (this.isPermitted(this.state.currentUser.permissions, ['roles:read'])) {
      authenticators.unshift(
        <LinkContainer key="roles" to={Routes.SYSTEM.AUTHENTICATION.ROLES}>
          <NavItem title="Roles">Roles</NavItem>
        </LinkContainer>,
      );
    }
    if (this.isPermitted(this.state.currentUser.permissions, ['users:list'])) {
      authenticators.unshift(
        <LinkContainer key="users" to={Routes.SYSTEM.AUTHENTICATION.USERS.LIST}>
          <NavItem title="Users">Users</NavItem>
        </LinkContainer>,
      );
    }

    if (authenticators.length === 0) {
      // special case, this is a user editing their own profile
      authenticators = [<LinkContainer key="profile-edit" to={Routes.SYSTEM.AUTHENTICATION.USERS.edit(encodeURIComponent(this.state.currentUser.username))}>
        <NavItem title="Edit User">Edit User</NavItem>
      </LinkContainer>];
    }
    const subnavigation = (
      <Nav stacked bsStyle="pills">
        {authenticators}
      </Nav>
    );

    const contentComponent = React.Children.count(this.props.children) === 1 ? React.Children.only(this.props.children) : this._contentComponent();

    return (<Row>
      <Col md={2} className={AuthenticationComponentStyle.subnavigation}>{subnavigation}</Col>
      <Col md={10} className={AuthenticationComponentStyle.contentpane}>{contentComponent}</Col>
    </Row>);
  },
});

export default AuthenticationComponent;
