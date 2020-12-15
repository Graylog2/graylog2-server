/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';

import Routes from 'routing/Routes';
import { Button, Alert, Table, Modal } from 'components/graylog';
import { Spinner } from 'components/common';
import { BootstrapModalConfirm } from 'components/bootstrap';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import CombinedProvider from 'injection/CombinedProvider';

import EditConfigurationVariableModal from './EditConfigurationVariableModal';
import ConfigurationHelperStyle from './ConfigurationHelper.css';

const { ConfigurationVariableActions } = CombinedProvider.get('ConfigurationVariable');

class ConfigurationVariablesHelper extends React.Component {
  static propTypes = {
    onVariableRename: PropTypes.func.isRequired,
  };

  state = {
    configurationVariables: undefined,
    errorModalContent: {},
    variableToDelete: {},
  };

  componentDidMount() {
    this._reloadVariables();
  }

  _reloadVariables = () => {
    ConfigurationVariableActions.all()
      .then((configurationVariables) => {
        this.setState({ configurationVariables: configurationVariables });
      });
  };

  _closeErrorModal = () => {
    this.errorModal.close();
  };

  _renderConfigList = (configurations) => {
    return (
      <ul className={ConfigurationHelperStyle.ulStyle}>
        {configurations.map((conf) => <li key={conf.id}><a href={Routes.SYSTEM.SIDECARS.EDIT_CONFIGURATION(conf.id)}>{conf.name}</a></li>)}
      </ul>
    );
  };

  _handleDeleteConfirm = () => {
    const { variableToDelete } = this.state;

    ConfigurationVariableActions.delete(variableToDelete)
      .then(() => this._onSuccessfulUpdate(() => this.deleteConfirmModal.close()));
  };

  _handleDeleteCheck = (configVar) => {
    return () => {
      this.setState({ variableToDelete: configVar });

      ConfigurationVariableActions.getConfigurations(configVar).then((response) => {
        // Variable still in use: Report error
        if (response.length > 0) {
          this.setState({ errorModalContent: this._renderConfigList(response) });
          this.errorModal.open();
          // Not in use, ask for confirmation
        } else {
          this.deleteConfirmModal.open();
        }
      });
    };
  };

  _configurationVariableListBuilder = () => {
    const variableRows = [];
    const { configurationVariables } = this.state;

    Object.values(configurationVariables).forEach((configVar) => {
      const escapedName = `\${user.${configVar.name}}`;

      variableRows.push(
        <tr key={configVar.id}>
          <td><code>{escapedName}</code></td>
          <td>{configVar.description}</td>
          <td>
            <Button bsStyle="primary" bsSize="xsmall" onClick={this._handleDeleteCheck(configVar)}>
              Delete
            </Button>
            &nbsp;
            <EditConfigurationVariableModal id={configVar.id}
                                            name={configVar.name}
                                            description={configVar.description}
                                            content={configVar.content}
                                            create={false}
                                            saveConfigurationVariable={this._saveConfigurationVariable} />
          </td>
        </tr>,
      );
    });

    return variableRows;
  };

  _isLoading = () => {
    const { configurationVariables } = this.state;

    return !configurationVariables;
  };

  _saveConfigurationVariable = (configurationVariable, oldName, callback) => {
    const { onVariableRename } = this.props;

    ConfigurationVariableActions.save.triggerPromise(configurationVariable)
      .then(() => this._onSuccessfulUpdate(() => {
        onVariableRename(oldName, configurationVariable.name);
        callback();
      }));
  };

  _onSuccessfulUpdate = (callback) => {
    if (typeof callback === 'function') {
      callback();
    }

    this._reloadVariables();
  };

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { variableToDelete, errorModalContent } = this.state;

    return (
      <div>
        <EditConfigurationVariableModal create
                                        saveConfigurationVariable={this._saveConfigurationVariable} />
        <div className="clearfix" />
        <div className={`table-responsive ${ConfigurationHelperStyle.tableMaxHeight}`}>
          <Table responsive>
            <thead>
              <tr>
                <th>Name</th>
                <th>Description</th>
                <th className={ConfigurationHelperStyle.actionsColumn}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {this._configurationVariableListBuilder()}
            </tbody>
          </Table>
        </div>

        <BootstrapModalWrapper ref={(modal) => { this.errorModal = modal; }}>
          <Modal.Header>
            <Modal.Title>Error deleting configuration variable <strong>$&#123;user.{variableToDelete.name}&#125;</strong></Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Alert bsStyle="warning">
              <p>
                Cannot delete this configuration variable as it is still in use. Please remove the variable from
                the following configurations and try again.
                {errorModalContent}
              </p>
            </Alert>
          </Modal.Body>
          <Modal.Footer>
            <Button onClick={this._closeErrorModal}>Close</Button>
          </Modal.Footer>
        </BootstrapModalWrapper>

        <BootstrapModalConfirm ref={(c) => { this.deleteConfirmModal = c; }}
                               title="Delete Configuration Variable?"
                               onConfirm={this._handleDeleteConfirm}
                               onCancel={this._closeErrorModal}>
          <p>Are you sure you want to remove the configuration variable <strong>{variableToDelete.name}</strong>?</p>
        </BootstrapModalConfirm>
      </div>
    );
  }
}

export default ConfigurationVariablesHelper;
