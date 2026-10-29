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
import React from 'react';
import { Trans, withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import Routes from 'routing/Routes';
import { ClearFloat, Spinner } from 'components/common';
import { Button, Alert, Table, Modal, BootstrapModalConfirm, BootstrapModalWrapper } from 'components/bootstrap';
import {
  deleteConfigurationVariable,
  fetchAllConfigurationVariables,
  getConfigurationsForVariable,
  saveConfigurationVariable,
} from 'api/configuration-variables';

import EditConfigurationVariableModal from './EditConfigurationVariableModal';
import ConfigurationHelperStyle from './ConfigurationHelper.css';

const _renderConfigList = (configurations) => (
  <ul className={ConfigurationHelperStyle.ulStyle}>
    {configurations.map((conf) => (
      <li key={conf.id}>
        <a href={Routes.SYSTEM.SIDECARS.EDIT_CONFIGURATION(conf.id)}>{conf.name}</a>
      </li>
    ))}
  </ul>
);

type ConfigurationVariablesHelperProps = {
  onVariableRename: (...args: any[]) => void;
} & WithTranslation;

type ConfigurationVariable = {
  id: string;
  name: string;
  description: string;
  content: string;
};

type ConfigurationVariablesHelperState = {
  configurationVariables: { [key: string]: ConfigurationVariable };
  showModal: boolean;
  showConfirmModal: boolean;
  errorModalContent: React.ReactElement;
  variableToDelete: ConfigurationVariable;
};

class ConfigurationVariablesHelper extends React.Component<
  ConfigurationVariablesHelperProps,
  ConfigurationVariablesHelperState
> {
  constructor(props) {
    super(props);

    this.state = {
      showModal: false,
      showConfirmModal: false,
      configurationVariables: undefined,
      errorModalContent: undefined,
      variableToDelete: undefined,
    };
  }

  componentDidMount() {
    this._reloadVariables();
  }

  _reloadVariables = () => {
    fetchAllConfigurationVariables().then((configurationVariables: any) => {
      this.setState({ configurationVariables: configurationVariables });
    });
  };

  _openErrorModal = () => {
    this.setState({ showModal: true });
  };

  _openErrorConfirmModal = () => {
    this.setState({ showConfirmModal: true });
  };

  _closeErrorModal = () => {
    this.setState({ showModal: false, showConfirmModal: false });
  };

  _handleDeleteConfirm = () => {
    const { variableToDelete } = this.state;

    deleteConfigurationVariable(variableToDelete).then(() => this._onSuccessfulUpdate(() => this._closeErrorModal()));
  };

  _handleDeleteCheck = (configVar) => () => {
    this.setState({ variableToDelete: configVar });

    getConfigurationsForVariable(configVar).then((response: any) => {
      // Variable still in use: Report error
      if (response.length > 0) {
        this.setState({ errorModalContent: _renderConfigList(response) });
        this._openErrorModal();
        // Not in use, ask for confirmation
      } else {
        this._openErrorConfirmModal();
      }
    });
  };

  _configurationVariableListBuilder = () => {
    const variableRows = [];
    const { configurationVariables } = this.state;

    Object.values(configurationVariables).forEach((configVar) => {
      const escapedName = `\${user.${configVar.name}}`;

      variableRows.push(
        <tr key={configVar.id}>
          <td>
            <code>{escapedName}</code>
          </td>
          <td>{configVar.description}</td>
          <td>
            <Button bsStyle="danger" bsSize="xsmall" onClick={this._handleDeleteCheck(configVar)}>
              {this.props.t('configuration_forms.configuration_variables_helper.delete')}
            </Button>
            &nbsp;
            <EditConfigurationVariableModal
              id={configVar.id}
              name={configVar.name}
              description={configVar.description}
              content={configVar.content}
              create={false}
              saveConfigurationVariable={this._saveConfigurationVariable}
            />
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

    saveConfigurationVariable(configurationVariable).then(() =>
      this._onSuccessfulUpdate(() => {
        onVariableRename(oldName, configurationVariable.name);
        callback();
      }),
    );
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

    const { variableToDelete, errorModalContent, showModal, showConfirmModal } = this.state;
    const { t } = this.props;

    return (
      <div>
        <EditConfigurationVariableModal create saveConfigurationVariable={this._saveConfigurationVariable} />
        <ClearFloat />
        <div className={`table-responsive ${ConfigurationHelperStyle.tableMaxHeight}`}>
          <Table responsive>
            <thead>
              <tr>
                <th>{t('configuration_forms.configuration_variables_helper.name')}</th>
                <th>{t('configuration_forms.configuration_variables_helper.description')}</th>
                <th className={ConfigurationHelperStyle.actionsColumn}>
                  {t('configuration_forms.configuration_variables_helper.actions')}
                </th>
              </tr>
            </thead>
            <tbody>{this._configurationVariableListBuilder()}</tbody>
          </Table>
        </div>

        <BootstrapModalWrapper showModal={showModal} onHide={this._closeErrorModal}>
          <Modal.Header>
            <Modal.Title>
              <Trans
                t={t}
                i18nKey="configuration_forms.configuration_variables_helper.error_delete_title"
                components={{ strong: <strong /> }}
                values={{ name: variableToDelete?.name }}
              />
            </Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Alert bsStyle="warning">
              <p>
                {t('configuration_forms.configuration_variables_helper.error_in_use')}
                {errorModalContent}
              </p>
            </Alert>
          </Modal.Body>
          <Modal.Footer>
            <Button onClick={this._closeErrorModal}>
              {t('configuration_forms.configuration_variables_helper.close')}
            </Button>
          </Modal.Footer>
        </BootstrapModalWrapper>

        <BootstrapModalConfirm
          showModal={showConfirmModal}
          title={t('configuration_forms.configuration_variables_helper.delete_confirm_title')}
          onConfirm={this._handleDeleteConfirm}
          onCancel={this._closeErrorModal}>
          <p>
            <Trans
              t={t}
              i18nKey="configuration_forms.configuration_variables_helper.delete_confirm_text"
              components={{ strong: <strong /> }}
              values={{ name: variableToDelete?.name }}
            />
          </p>
        </BootstrapModalConfirm>
      </div>
    );
  }
}

export default withTranslation('sidecars')(ConfigurationVariablesHelper);
