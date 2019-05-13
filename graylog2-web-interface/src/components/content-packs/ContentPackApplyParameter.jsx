import React from 'react';
import PropTypes from 'prop-types';
import { findIndex } from 'lodash';

import { Button, Col, Row } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import DataTable from 'components/common/DataTable';
import ValueReferenceData from 'util/ValueReferenceData';
import naturalSort from 'javascript-natural-sort';

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
      configKey: '',
      parameter: '',
    };
  }

  _configKeyRowFormatter = (paramMap) => {
    const { appliedParameter } = this.props;
    const enableClear = findIndex(appliedParameter,
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
    const { configKey, parameter } = this.state;
    return parameter.length > 0 && configKey.length > 0;
  };

  _applyParameter = (e) => {
    e.preventDefault();
    if (!this._valuesSelected()) {
      return;
    }
    const { configKey, parameter } = this.state;
    const { onParameterApply, appliedParameter: appliedParameter1 } = this.props;
    const configKeyIndex = appliedParameter1.findIndex((appliedParameter) => {
      return appliedParameter.configKey === configKey;
    });
    if (configKeyIndex >= 0) {
      return;
    }
    onParameterApply(configKey, parameter);
    this.setState({ configKey: '', parameter: '' });
  };

  _parameterClear = (configKey) => {
    const { onParameterClear } = this.props;
    onParameterClear(configKey);
  };

  render() {
    const { appliedParameter, entity } = this.props;
    const { configKey: configKeyState, parameter: parameter1 } = this.state;
    const vRefData = new ValueReferenceData(entity.data);
    const configPaths = vRefData.getPaths();

    const configKeys = Object.keys(configPaths)
      .sort(naturalSort)
      .filter(configKey => configPaths[configKey].isValueRef()) // Only allow value-refs as parameters
      .filter((configKey) => {
        return appliedParameter.findIndex((paramMap) => {
          return configKey === paramMap.configKey;
        }) < 0;
      });
    const emptyOption = (name) => { return (<option key="EMPTY" value="">{name}</option>); };
    const configOptions = [emptyOption('Choose Config Key')].concat(configKeys.map(key => <option key={key} value={key}>{key}</option>));
    let { parameters } = this.props;
    let emptyName = parameters.length <= 0 ? 'Create a parameter first' : 'Choose...';
    if (configKeyState !== '' && parameters.length > 0) {
      const configKeyType = configPaths[configKeyState].getValueType();
      if (['string', 'integer', 'boolean', 'double'].findIndex(type => type === configKeyType) >= 0) {
        parameters = parameters.filter(parameter => parameter.type === configKeyType);
      }
      emptyName = parameters.length <= 0 ? `No parameter from type ${configKeyType}` : 'Choose...';
    }
    const parameterOptions = [emptyOption(emptyName)]
      .concat(parameters.map(key => <option key={key.name} value={key.name}>{key.title} ({key.name})</option>));
    return (
      <div>
        <form className="apply-parameter-form" id="apply-parameter-form" onSubmit={this._applyParameter}>
          <Row className={Style.applyParameter}>
            <Col sm={6}>
              <Input name="configKey"
                     id="configKey"
                     type="select"
                     value={configKeyState}
                     onChange={this._bindValue}
                     label="Config Key"
                     required>
                {configOptions}
              </Input>
            </Col>
            <Col sm={6}>
              <Input name="parameter"
                     id="parameter"
                     type="select"
                     value={parameter1}
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
              <Button className="pull-right" bsStyle="primary" disabled={!this._valuesSelected()} type="submit">Apply</Button>
            </Col>
          </Row>
        </form>
        <Row>
          <Col>
            <DataTable
              wrapperClassName={Style.applyTable}
              id="config-key-list"
              headers={['Config Key', 'Parameter', 'Action']}
              filterKeys={[]}
              rows={appliedParameter}
              dataRowFormatter={this._configKeyRowFormatter}
            />
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackApplyParameter;
