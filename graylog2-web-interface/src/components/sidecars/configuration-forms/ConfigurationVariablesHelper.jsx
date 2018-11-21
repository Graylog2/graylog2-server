import React from 'react';
import { Table } from 'react-bootstrap';
import { Spinner } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';
import EditConfigurationVariableModal from './EditConfigurationVariableModal';
import ConfigurationHelperStyle from './ConfigurationHelper.css';
import DeleteConfirmButton from './DeleteConfirmButton';

const { ConfigurationVariableActions } = CombinedProvider.get('ConfigurationVariable');

class ConfigurationVariablesHelper extends React.Component {
  state = {
    configurationVariables: [],
  };

  componentDidMount() {
    this._reloadConfiguration();
  }

  rightAlignStyle = { textAlign: 'right' };

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

  _configurationVariableListFormatter = () => {
    const variableRows = [];
    Object.values(this.state.configurationVariables).forEach((configurationVar) => {
      const escapedName = `\${${configurationVar.name}}`;
      variableRows.push(
        <tr key={this._getId(configurationVar.id)}>
          <td><code>{escapedName}</code></td>
          <td>{configurationVar.description}</td>
          <td style={this.rightAlignStyle}>
            <DeleteConfirmButton entity={configurationVar} type="Variable" onClick={this._deleteConfigurationVariable} />
            <EditConfigurationVariableModal id={configurationVar.id}
                                            name={configurationVar.name}
                                            description={configurationVar.description}
                                            content={configurationVar.content}
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

  _deleteConfigurationVariable = (configurationVariable, callback) => {
    ConfigurationVariableActions.delete.triggerPromise(configurationVariable)
      .then(() => this._onSuccessfulUpdate(callback));
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
                <th style={this.rightAlignStyle}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {this._configurationVariableListFormatter()}
            </tbody>
          </Table>
        </div>
      </div>
    );
  }
}

export default ConfigurationVariablesHelper;
