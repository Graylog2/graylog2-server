// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import uuid from 'uuid/v4';
import connect from 'stores/connect';
import { Button } from 'components/graylog';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import UrlWhiteListForm from 'components/configurations/UrlWhiteListForm';
import CombinedProvider from 'injection/CombinedProvider';
import PermissionsMixin from 'util/PermissionsMixin';
import type { WhiteListConfig } from 'stores/configurations/ConfigurationsStore';
import IfPermitted from 'components/common/IfPermitted';

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
  urlType: 'regex' | 'literal',
};


class URLWhiteListFormModal extends React.Component<Props, State> {
    configModal: ?BootstrapModalForm = React.createRef();

    inputs = {};

    static defaultProps = {
      newUrlEntry: '',
      onUpdate: () => {},
      configuration: {},
      urlType: '',
    }

    constructor(props) {
      super(props);
      this.state = {
        config: { entries: [], disabled: false },
        isValid: false,
      };
      this.configModal = React.createRef();
    }

    componentDidMount() {
      const { currentUser: { permissions } } = this.props;
      if (PermissionsMixin.isPermitted(permissions, ['urlwhitelist:read'])) {
        ConfigurationsActions.listWhiteListConfig(URL_WHITELIST_CONFIG);
      }
    }

    componentDidUpdate(prevProps) {
      const { config: { entries } } = this.state;
      const { newUrlEntry } = this.props;
      const urlwhitelistConfig = this._getConfig(URL_WHITELIST_CONFIG);
      if (urlwhitelistConfig && entries.length === 0) {
        this._setDefaultWhiteListState(urlwhitelistConfig);
      } else if (prevProps.newUrlEntry !== newUrlEntry) {
        this._setDefaultWhiteListState(urlwhitelistConfig);
      }
    }

  _setDefaultWhiteListState =(urlwhitelistConfig) => {
    const { newUrlEntry, urlType } = this.props;
    const { isValid } = this.state;
    const config = { entries: [...urlwhitelistConfig.entries, { id: uuid(), title: '', value: newUrlEntry, type: urlType || 'literal' }], disabled: urlwhitelistConfig.disabled };
    this._update(config, isValid);
  }

  _getConfig = (configType: string): Object => {
    const { configuration } = this.props;
    if (configuration && configuration[configType]) {
      return configuration[configType];
    }
    return null;
  }

  _openModal = () => {
    if (this.configModal) {
      this.configModal.current.open();
    }
  }

  _closeModal = () => {
    if (this.configModal) {
      this.configModal.current.close();
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

  _updateConfig = (configType, config) => {
    switch (configType) {
      case URL_WHITELIST_CONFIG:
        return ConfigurationsActions.updateWhitelist(configType, config);
      default:
        return ConfigurationsActions.update(configType, config);
    }
  };

  _resetConfig = () => {
    const urlwhitelistConfig = this._getConfig(URL_WHITELIST_CONFIG);
    this._setDefaultWhiteListState(urlwhitelistConfig);
  }

  render() {
    const urlwhitelistConfig = this._getConfig(URL_WHITELIST_CONFIG);
    if (urlwhitelistConfig) {
      const { isValid, config: { entries, disabled } } = this.state;
      return (
        <>
          <IfPermitted permissions="urlwhitelist:write">
            <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Add to URL Whitelist</Button>
          </IfPermitted>
          <BootstrapModalForm ref={this.configModal}
                              bsSize="lg"
                              title="Update Whitelist Configuration"
                              onSubmitForm={this._saveConfig}
                              onModalClose={this._resetConfig}
                              submitButtonDisabled={!isValid}
                              submitButtonText="Save">
            <h3>Whitelist URLs</h3>
            <UrlWhiteListForm urls={entries} disabled={disabled} onUpdate={this._update} />
          </BootstrapModalForm>
        </>
      );
    }
    return null;
  }
}

URLWhiteListFormModal.propTypes = {
  newUrlEntry: PropTypes.string,
  onUpdate: PropTypes.func,
  configuration: PropTypes.object,
  currentUser: PropTypes.object.isRequired,
  urlType: PropTypes.string,
};

export default connect(URLWhiteListFormModal, { configurations: ConfigurationsStore, currentUser: CurrentUserStore }, ({ configurations, currentUser, ...otherProps }) => ({
  ...configurations,
  ...currentUser,
  ...otherProps,
}));
