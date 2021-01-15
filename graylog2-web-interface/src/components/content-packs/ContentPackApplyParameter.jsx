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
import PropTypes from 'prop-types';
import { findIndex } from 'lodash';
import naturalSort from 'javascript-natural-sort';

import { Row, Col, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import DataTable from 'components/common/DataTable';
import ValueReferenceData from 'util/ValueReferenceData';

import Style from './ContentPackApplyParameter.css';

class ContentPackApplyParameter extends React.Component {
  static propTypes = {
    onParameterApply: PropTypes.func,
    onParameterClear: PropTypes.func,
    entity: PropTypes.object.isRequired,
    parameters: PropTypes.array,
    appliedParameter: PropTypes.array,
  };

  static defaultProps = {
    onParameterApply: () => {},
    onParameterClear: () => {},
    parameters: [],
    appliedParameter: [],
  };

  constructor(props) {
    super(props);

    this.state = {
      config_key: '',
      parameter: '',
    };
  }

  _configKeyRowFormatter = (paramMap) => {
    const enableClear = findIndex(this.props.appliedParameter,
      { paramName: paramMap.paramName, configKey: paramMap.configKey, readOnly: true }) < 0;
    const lastCol = enableClear
      ? <td><Button bsStyle="info" bsSize="small" onClick={() => { this._parameterClear(paramMap.configKey); }}>Clear</Button></td>
      : <td />;

    return (
      <tr key={paramMap.configKey}>
        <td>{paramMap.configKey}</td>
        <td>{paramMap.paramName}</td>
        { lastCol }
      </tr>
    );
  };

  _bindValue = (event) => {
    const newValue = {};

    newValue[event.target.name] = event.target.value;
    this.setState(newValue);
  };

  _valuesSelected = () => {
    return this.state.parameter.length > 0 && this.state.config_key.length > 0;
  };

  _applyParameter = (e) => {
    e.preventDefault();

    if (!this._valuesSelected()) {
      return;
    }

    const configKeyIndex = this.props.appliedParameter.findIndex((appliedParameter) => {
      return appliedParameter.configKey === this.state.config_key;
    });

    if (configKeyIndex >= 0) {
      return;
    }

    this.props.onParameterApply(this.state.config_key, this.state.parameter);
    this.setState({ config_key: '', parameter: '' });
  };

  _parameterClear = (configKey) => {
    this.props.onParameterClear(configKey);
  };

  render() {
    const vRefData = new ValueReferenceData(this.props.entity.data);
    const configPaths = vRefData.getPaths();

    const configKeys = Object.keys(configPaths)
      .sort(naturalSort)
      .filter((configKey) => configPaths[configKey].isValueRef()) // Only allow value-refs as parameters
      .filter((configKey) => {
        return this.props.appliedParameter.findIndex((paramMap) => {
          return configKey === paramMap.configKey;
        }) < 0;
      });
    const emptyOption = (name) => { return (<option key="EMPTY" value="">{name}</option>); };
    const configOptions = [emptyOption('Choose Config Key')].concat(configKeys.map((key) => <option key={key} value={key}>{key}</option>));
    let { parameters } = this.props;
    let emptyName = parameters.length <= 0 ? 'Create a parameter first' : 'Choose...';

    if (this.state.config_key !== '' && parameters.length > 0) {
      const configKeyType = configPaths[this.state.config_key].getValueType();

      if (['string', 'integer', 'boolean', 'double'].findIndex((type) => type === configKeyType) >= 0) {
        parameters = parameters.filter((parameter) => parameter.type === configKeyType);
      }

      emptyName = parameters.length <= 0 ? `No parameter from type ${configKeyType}` : 'Choose...';
    }

    const parameterOptions = [emptyOption(emptyName)]
      .concat(parameters.map((key) => <option key={key.name} value={key.name}>{key.title} ({key.name})</option>));

    return (
      <div>
        <form className="apply-parameter-form" id="apply-parameter-form" onSubmit={this._applyParameter}>
          <Row className={Style.applyParameter}>
            <Col smOffset={1} sm={5}>
              <Input name="config_key"
                     id="config_key"
                     type="select"
                     value={this.state.config_key}
                     onChange={this._bindValue}
                     label="Config Key"
                     required>
                {configOptions}
              </Input>
            </Col>
            <Col sm={5}>
              <Input name="parameter"
                     id="parameter"
                     type="select"
                     value={this.state.parameter}
                     onChange={this._bindValue}
                     label="Parameter"
                     required>
                {parameterOptions}
              </Input>
            </Col>
            <Col sm={1} />
          </Row>
          <Row>
            <Col smOffset={10} sm={2}>
              <Button bsStyle="primary" disabled={!this._valuesSelected()} type="submit">Apply</Button>
            </Col>
          </Row>
        </form>
        <Row>
          <Col smOffset={1} sm={10}>
            <DataTable id="config-key-list"
                       headers={['Config Key', 'Parameter', 'Action']}
                       filterKeys={[]}
                       rows={this.props.appliedParameter}
                       dataRowFormatter={this._configKeyRowFormatter} />
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackApplyParameter;
