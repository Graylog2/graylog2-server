// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { Button, Alert, Table } from 'components/graylog';
import { IfPermitted, Select, Icon } from 'components/common';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Input from 'components/bootstrap/Input';

import ObjectUtils from 'util/ObjectUtils';

class UrlWhiteListConfig extends React.Component {
  configModal: null;

  constructor(props) {
    super(props);

    this.inputs = [];
    const { config } = this.props;
    this.state = {
      config,
    };
  }

  _summary = () => {
    const { config: { entries } } = this.props;
    return entries.map((urlConfig, idx) => {
      return (
        <tr>
          <td>{idx + 1}</td>
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

  _onInputChange = (event, idx) => {
    const { config } = this.state;
    const update = ObjectUtils.clone(config);
    update.entries[idx].value = this.inputs[`ref${idx}`].input.value;
    this.setState({ config: update });
  }

  _onUpdateUrl = (type, value) => {
    const { config } = this.state;
    const update = ObjectUtils.clone(config);
    Object.assign(update.entries[update.entries.findIndex(el => el.value === value)] = { value, type });
    this.setState({ config: update });
  }

  _saveConfig = () => {
    const { config } = this.state;
    const { updateConfig } = this.props;
    updateConfig(config).then(() => {
      this._closeModal();
    });
  }

  _onRemove = (idx) => {
    return () => {
      const { config: { entries } } = this.state;
      const update = ObjectUtils.clone(entries);
      update.splice(idx, 1);
      this.setState({
        config: {
          entries: update,
        },
      });
    };
  }

  _urlWhiteListForm = () => {
    const { config: { entries } } = this.state;
    const options = [{ value: 'literal', label: 'Literal' }, { value: 'regex', label: 'Regex' }];
    return entries.map((url, idx) => {
      return (
        // eslint-disable-next-line react/no-array-index-key
        <tr key={idx + 1}>
          <td>{idx + 1}</td>
          <td>
            <Input type="text"
                   ref={(elem) => { this.inputs[`ref${idx}`] = elem; }}
                   onChange={event => this._onInputChange(event, idx)}
                   defaultValue={url.value}
                   required />
          </td>
          <td>
            <Input required
                   autoFocus>
              <Select placeholder="Select Cache Type"
                      clearable={false}
                      options={options}
                      matchProp="label"
                      onChange={option => this._onUpdateUrl(option, url.value)}
                      value={url.type} />
            </Input>
          </td>
          <td>
            <span className="">
              <Icon name="fa-trash" style={{ cursor: 'pointer' }} onClick={this._onRemove(idx)} />
            </span>
          </td>
        </tr>
      );
    });
  }

  _resetConfig = () => {
    const { config } = this.props;
    this.setState({
      config,
    });
  }

  _onAdd = () => {
    const { config: { entries } } = this.state;
    const update = ObjectUtils.clone(entries);
    update.push({ value: '' });
    this.setState({
      config: {
        entries: update,
      },
    });
  };

  render() {
    return (
      <div>
        <h2>URL Whitelist Configuration</h2>

        <Table striped bordered condensed className="top-margin">
          <thead>
            <tr>
              <th>#</th>
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
                            title="Update White List Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">

          <h3>Urls</h3>
          <Table striped bordered condense className="top-margin">
            <thead>
              <tr>
                <th>#</th>
                <th>Url</th>
                <th>Type</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {this._urlWhiteListForm()}
            </tbody>
          </Table>
          <Button bsSize="xs" onClick={this._onAdd}>Add Url</Button>
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
