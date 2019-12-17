// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Button, Table } from 'components/graylog';
import { IfPermitted } from 'components/common';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import type { Config } from 'stores/configurations/ConfigurationsStore';
import UrlWhiteListForm from './UrlWhiteListForm';

type State = {
  config: Config,
  valid: boolean
};

type Props = {
  config: Config,
  updateConfig: (config: Config) => Promise<void>,
};
class UrlWhiteListConfig extends React.Component<Props, State> {
  configModal: ?BootstrapModalForm;

  inputs = {};

  constructor(props: Props) {
    super(props);
    const { config } = this.props;
    this.state = {
      config,
      valid: false,
    };
  }

  _summary = (): React.Element<'tr'>[] => {
    const { config: { entries } } = this.props;
    return entries.map((urlConfig, idx) => {
      return (
        <tr key={urlConfig.id}>
          <td>{idx + 1}</td>
          <td>{urlConfig.title}</td>
          <td>{urlConfig.value}</td>
          <td>{urlConfig.type}</td>
        </tr>
      );
    });
  }

  _openModal = () => {
    this.configModal.open();
  }

  _closeModal = () => {
    this.configModal.close();
  }

  _saveConfig = () => {
    const { config, valid } = this.state;
    const { updateConfig } = this.props;
    if (valid) {
      updateConfig(config).then(() => {
        this._closeModal();
      });
    }
  }

  _update = (config: Config, valid: boolean) => {
    const updatedState = { config, valid };
    this.setState(updatedState);
  }


  _resetConfig = () => {
    const { config } = this.props;
    const updatedState = { ...this.state, config };
    this.setState(updatedState);
  }


  render() {
    const { config: { entries, disabled } } = this.props;
    return (
      <div>
        <h2>URL Whitelist Configuration  {disabled ? <small>(Disabled)</small> : <small>(Enabled)</small> }</h2>

        <Table striped bordered condensed className="top-margin">
          <thead>
            <tr>
              <th>#</th>
              <th>Title</th>
              <th>URL</th>
              <th>Type</th>
            </tr>
          </thead>
          <tbody>
            {this._summary()}
          </tbody>
        </Table>
        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
        </IfPermitted>

        <BootstrapModalForm ref={(configModal) => { this.configModal = configModal; }}
                            bsSize="lg"
                            title="Update White List Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">

          <h3>Urls</h3>
          <UrlWhiteListForm urls={entries} disabled={disabled} update={this._update} />
        </BootstrapModalForm>
      </div>
    );
  }
}

UrlWhiteListConfig.propTypes = {
  config: PropTypes.object.isRequired,
  updateConfig: PropTypes.func.isRequired,
};

export default UrlWhiteListConfig;
