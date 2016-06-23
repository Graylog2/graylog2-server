import React from 'react';
import Reflux from 'reflux';
import { Alert, Tabs, Tab } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import UserList from 'components/users/UserList';
import RolesComponent from 'components/users/RolesComponent';
import AuthProvidersConfig from './AuthProvidersConfig';

import ActionsProvider from 'injection/ActionsProvider';
const AuthenticationActions = ActionsProvider.getActions('Authentication');

import StoreProvider from 'injection/StoreProvider';
const AuthenticationStore = StoreProvider.getStore('Authentication');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');


const AuthenticationComponent = React.createClass({
  mixins: [Reflux.connect(AuthenticationStore), Reflux.connect(CurrentUserStore)],

  componentDidMount() {
    AuthenticationActions.load();

    PluginStore.exports('authenticatorConfigurations').forEach((authConfig) => {
      this.authenticatorConfigurations[authConfig.name] = authConfig;
      // TODO load per authenticator config
    });
  },

  // contains the 'authname' -> plugin descriptor
  authenticatorConfigurations: {},

  _pluginPane(name) {
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

  render() {
    let authenticators = <Tab disabled title="Loading..."/>;
    const auths = this.state.authenticators;
    if (auths) {
      authenticators = auths.realm_order.map((name, idx) => {
        const auth = this.authenticatorConfigurations[name];
        const title = (auth || { displayName: name }).displayName;
        return <Tab key={name} eventKey={name} title={`${idx + 1}. ${title}`}>{this._pluginPane(name)}</Tab>;
      });

      // settings
      authenticators.push(
        <Tab key="settings" eventKey="settings" title="Configure Providers">
          <AuthProvidersConfig config={this.state.authenticators}
                               descriptors={this.authenticatorConfigurations}
                               updateConfig={this._onUpdateProviders} />
        </Tab>
      );
    }

    return (
      <Tabs defaultActiveKey={"users"} position="left" tabWidth={2}>
        <Tab eventKey={"users"} title="Users">
          <UserList currentUsername={this.state.currentUser.username} currentUser={this.state.currentUser}/>
        </Tab>
        <Tab eventKey="roles" title="Roles">
          <RolesComponent />
        </Tab>
        {authenticators}
      </Tabs>
    );
  },
});

export default AuthenticationComponent;
