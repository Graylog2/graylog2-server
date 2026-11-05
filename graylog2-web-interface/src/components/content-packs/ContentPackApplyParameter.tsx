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
import findIndex from 'lodash/findIndex';

import i18n from 'i18n';
import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { Row, Col, Button, Input } from 'components/bootstrap';
import DataTable from 'components/common/DataTable';
import ValueReferenceData from 'util/ValueReferenceData';

import Style from './ContentPackApplyParameter.css';

const t = (key: string, options?: { [key: string]: unknown }) => i18n.t(key, { ns: 'content_packs', ...options });

type ContentPackApplyParameterProps = {
  onParameterApply?: (...args: any[]) => void;
  onParameterClear?: (...args: any[]) => void;
  entity: any;
  parameters?: any[];
  appliedParameter?: any[];
};

class ContentPackApplyParameter extends React.Component<
  ContentPackApplyParameterProps,
  {
    [key: string]: any;
  }
> {
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
    const enableClear =
      findIndex(this.props.appliedParameter, {
        paramName: paramMap.paramName,
        configKey: paramMap.configKey,
        readOnly: true,
      }) < 0;
    const lastCol = enableClear ? (
      <td>
        <Button
          bsStyle="info"
          bsSize="small"
          onClick={() => {
            this._parameterClear(paramMap.configKey);
          }}>
          {t('apply_parameter.clear')}
        </Button>
      </td>
    ) : (
      <td />
    );

    return (
      <tr key={paramMap.configKey}>
        <td>{paramMap.configKey}</td>
        <td>{paramMap.paramName}</td>
        {lastCol}
      </tr>
    );
  };

  _bindValue = (event) => {
    const newValue = {};

    newValue[event.target.name] = event.target.value;
    this.setState(newValue);
  };

  _valuesSelected = () => this.state.parameter.length > 0 && this.state.config_key.length > 0;

  _applyParameter = (e) => {
    e.preventDefault();

    if (!this._valuesSelected()) {
      return;
    }

    const configKeyIndex = this.props.appliedParameter.findIndex(
      (appliedParameter) => appliedParameter.configKey === this.state.config_key,
    );

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
      .filter((configKey) => this.props.appliedParameter.findIndex((paramMap) => configKey === paramMap.configKey) < 0);
    const emptyOption = (name) => (
      <option key="EMPTY" value="">
        {name}
      </option>
    );
    const configOptions = [emptyOption(t('apply_parameter.choose_config_key'))].concat(
      configKeys.map((key) => (
        <option key={key} value={key}>
          {key}
        </option>
      )),
    );
    let { parameters } = this.props;
    let emptyName = parameters.length <= 0 ? t('apply_parameter.create_parameter_first') : t('apply_parameter.choose');

    if (this.state.config_key !== '' && parameters.length > 0) {
      const configKeyType = configPaths[this.state.config_key].getValueType();

      if (['string', 'integer', 'boolean', 'double'].findIndex((type) => type === configKeyType) >= 0) {
        parameters = parameters.filter((parameter) => parameter.type === configKeyType);
      }

      emptyName =
        parameters.length <= 0
          ? t('apply_parameter.no_parameter_of_type', { type: configKeyType })
          : t('apply_parameter.choose');
    }

    const parameterOptions = [emptyOption(emptyName)].concat(
      parameters.map((key) => (
        <option key={key.name} value={key.name}>
          {key.title} ({key.name})
        </option>
      )),
    );

    return (
      <div>
        <form className="apply-parameter-form" id="apply-parameter-form" onSubmit={this._applyParameter}>
          <Row className={Style.applyParameter}>
            <Col smOffset={1} sm={5}>
              <Input
                name="config_key"
                id="config_key"
                type="select"
                value={this.state.config_key}
                onChange={this._bindValue}
                label={t('apply_parameter.config_key')}
                required>
                {configOptions}
              </Input>
            </Col>
            <Col sm={5}>
              <Input
                name="parameter"
                id="parameter"
                type="select"
                value={this.state.parameter}
                onChange={this._bindValue}
                label={t('apply_parameter.parameter')}
                required>
                {parameterOptions}
              </Input>
            </Col>
            <Col sm={1} />
          </Row>
          <Row>
            <Col smOffset={10} sm={2}>
              <Button bsStyle="primary" disabled={!this._valuesSelected()} type="submit">
                {t('apply_parameter.apply')}
              </Button>
            </Col>
          </Row>
        </form>
        <Row>
          <Col smOffset={1} sm={10}>
            <DataTable
              id="config-key-list"
              headers={[t('apply_parameter.config_key'), t('apply_parameter.parameter'), t('apply_parameter.action')]}
              filterKeys={[]}
              rows={this.props.appliedParameter}
              dataRowFormatter={this._configKeyRowFormatter}
            />
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackApplyParameter;
