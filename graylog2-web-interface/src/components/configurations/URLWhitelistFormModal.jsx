// @flow
import React from 'react';
import PropTypes from 'prop-types';
import uuid from 'uuid/v4';
import connect from 'stores/connect';
import { IfPermitted } from 'components/common';
import { Button } from 'components/graylog';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import UrlWhiteListForm from 'components/configurations/UrlWhiteListForm';
import CombinedProvider from 'injection/CombinedProvider';
import PermissionsMixin from 'util/PermissionsMixin';
import type { WhiteListConfig } from 'stores/configurations/ConfigurationsStore';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');
const { ConfigurationsActions, ConfigurationsStore } = CombinedProvider.get('Configurations');

const URL_WHITELIST_CONFIG = 'org.graylog2.system.urlwhitelist.UrlWhitelist';

type State = {
  config: WhiteListConfig,
  isValid: boolean
};

type Props = {
  newUrlEntry: string,
  onUpdate: () => void,
  configuration: {},
  currentUser: {permissions: Array<string>},
  formType: string,
};


class URLWhitelistFormModal extends React.Component<Props, State> {
    configModal: ?BootstrapModalForm;

    inputs = {};

    static defaultProps = {
      newUrlEntry: '',
      onUpdate: () => {},
      configuration: {},
      formType: '',
    }

    constructor(props) {
      super(props);
      this.state = {
        config: { entries: [], disabled: false },
        isValid: false,
      };
    }

    componentDidMount() {
      const { currentUser: { permissions } } = this.props;
      if (PermissionsMixin.isPermitted(permissions, ['urlwhitelist:read'])) {
        ConfigurationsActions.listWhiteListConfig(URL_WHITELIST_CONFIG);
      }
    }


  _getConfig = (configType: string) => {
    const { configuration } = this.props;
    if (configuration && configuration[configType]) {
      return configuration[configType];
    }
    return null;
  }

  _openModal = () => {
    if (this.configModal) {
      this.configModal.open();
    }
  }

  _closeModal = () => {
    if (this.configModal) {
      this.configModal.close();
    }
  }

  _saveConfig = (event) => {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    const { onUpdate } = this.props;
    const { config, isValid } = this.state;
    if (isValid) {
      this._updateConfig(URL_WHITELIST_CONFIG, config).then(() => {
        onUpdate();
        this._closeModal();
      });
    }
  }

  _update = (config, isValid) => {
    const updatedState = { config, isValid };
    this.setState(updatedState);
  }


  _resetConfig = () => {

  }

  _updateConfig = (configType, config) => {
    switch (configType) {
      case URL_WHITELIST_CONFIG:
        return ConfigurationsActions.updateWhitelist(configType, config);
      default:
        return ConfigurationsActions.update(configType, config);
    }
  };


  render() {
    const urlwhitelistConfig = this._getConfig(URL_WHITELIST_CONFIG);
    if (urlwhitelistConfig) {
      const { newUrlEntry, formType } = this.props;
      const initialConfig = { entries: [...urlwhitelistConfig.entries, { id: uuid(), title: '', value: newUrlEntry, type: formType || 'literal' }], disabled: urlwhitelistConfig.disabled };
      const { entries, disabled } = initialConfig;
      const { isValid } = this.state;
      return (
        <>
          <IfPermitted permissions="urlwhitelist:write">
            <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Add to URL Whitelist</Button>
          </IfPermitted>
          <BootstrapModalForm ref={(configModal) => { this.configModal = configModal; }}
                              bsSize="lg"
                              title="Update Whitelist Configuration"
                              onSubmitForm={this._saveConfig}
                              onModalClose={this._resetConfig}
                              submitButtonDisabled={!isValid}
                              submitButtonText="Save">
            <h3>Whitelist URLs</h3>
            <UrlWhiteListForm urls={entries} disabled={disabled} onUpdate={this._update} newUrlEntry={newUrlEntry} />
          </BootstrapModalForm>
        </>
      );
    }
    return null;
  }
}

URLWhitelistFormModal.propTypes = {
  newUrlEntry: PropTypes.object,
  onUpdate: PropTypes.func,
  configuration: PropTypes.object,
  currentUser: PropTypes.object.isRequired,
  formType: PropTypes.string,
};

export default connect(URLWhitelistFormModal, { configurations: ConfigurationsStore, currentUser: CurrentUserStore }, ({ configurations, currentUser, ...otherProps }) => ({
  ...configurations,
  ...currentUser,
  ...otherProps,
}));
