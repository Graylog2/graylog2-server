import React from 'react';
import { Alert, Button, Table, Modal } from 'react-bootstrap';
import { Spinner } from 'components/common';
import { BootstrapModalConfirm } from 'components/bootstrap';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import CombinedProvider from 'injection/CombinedProvider';
import EditConfigurationVariableModal from './EditConfigurationVariableModal';
import ConfigurationHelperStyle from './ConfigurationHelper.css';

const { ConfigurationVariableActions } = CombinedProvider.get('ConfigurationVariable');

class ConfigurationVariablesHelper extends React.Component {
  state = {
    configurationVariables: [],
    errorModalContent: {},
  };

  componentDidMount() {
    this._reloadConfiguration();
  }

  _reloadConfiguration = () => {
    ConfigurationVariableActions.all()
      .then((configurationVariables) => {
        this.setState({ configurationVariables: configurationVariables });
      });
  };

  _getId = (idName, index) => {
    const idIndex = index !== undefined ? `. ${index}` : '';
    return idName + idIndex;
  };

  _renderConfigList = (configurations) => {
    return (<ul className={ConfigurationHelperStyle.ulStyle}>
      {configurations.map(conf => <li key={conf.id}><a href={conf.id}>{conf.name}</a></li>)}
    </ul>);
  };

  _configurationVariableListFormatter = () => {
    const variableRows = [];

    Object.values(this.state.configurationVariables).forEach((configVar) => {
      let deleteConfirmationModal;
      const _handleDeleteCheck = () => {
        ConfigurationVariableActions.getConfigurations(configVar).then((response) => {
          // Variable still in use: Report error
          if (response.length > 0) {
            this.setState({ errorModalContent: this._renderConfigList(response) });
            this.errorModal.open();
          // Not in use, ask for confirmation
          } else {
            deleteConfirmationModal.open();
          }
        });
      };

      const _handleDeleteConfirm = () => {
        ConfigurationVariableActions.delete(configVar)
          .then(() => this._onSuccessfulUpdate());
      };

      const escapedName = `\${${configVar.name}}`;
      variableRows.push(
        <tr key={this._getId(configVar.id)}>
          <td><code>{escapedName}</code></td>
          <td>{configVar.description}</td>
          <td style={{ textAlign: 'right' }}>
            <BootstrapModalConfirm ref={(c) => { deleteConfirmationModal = c; }}
                                   title="Delete Configuration Variable?"
                                   onConfirm={_handleDeleteConfirm}
                                   onCancel={() => {}}>
              <p>Are you sure you want to remove the configuration variable <strong>{configVar.name}</strong>?</p>
            </BootstrapModalConfirm>
            <Button bsStyle="primary" bsSize="xsmall" onClick={_handleDeleteCheck}>
              Delete
            </Button>
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
    return !(this.state.configurationVariables);
  };

  _saveConfigurationVariable = (configurationVariable, callback) => {
    ConfigurationVariableActions.save.triggerPromise(configurationVariable)
      .then(() => this._onSuccessfulUpdate(callback));
  };

  _onSuccessfulUpdate = (callback) => {
    if (typeof callback === 'function') {
      callback();
    }
    this._reloadConfiguration();
  };

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <div>
        <EditConfigurationVariableModal create
                                        saveConfigurationVariable={this._saveConfigurationVariable} />
        <br />
        <br />
        <br />
        <div className={`table-responsive ${ConfigurationHelperStyle.tableMaxHeight}`}>
          <Table responsive>
            <thead>
              <tr>
                <th>Name</th>
                <th>Description</th>
                <th style={{ textAlign: 'right' }} >Actions</th>
              </tr>
            </thead>
            <tbody>
              {this._configurationVariableListFormatter()}
              <BootstrapModalWrapper ref={(modal) => { this.errorModal = modal; }}>
                <Modal.Body>
                  <Alert bsStyle="warning">
                    <h4>Cannot delete this configuration variable. <br /> It is still used in the following configurations:</h4>
                    <p>
                      {this.state.errorModalContent}
                    </p>
                  </Alert>
                </Modal.Body>
                <Modal.Footer>
                  <Button onClick={() => { this.errorModal.close(); }}>Close</Button>
                </Modal.Footer>
              </BootstrapModalWrapper>
            </tbody>
          </Table>
        </div>
      </div>
    );
  }
}

export default ConfigurationVariablesHelper;
