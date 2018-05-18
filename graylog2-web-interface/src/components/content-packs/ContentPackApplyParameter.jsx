import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Row, Col, Button } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import DataTable from 'components/common/DataTable';
import ObjectUtils from 'util/ObjectUtils';

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
    return (
      <tr key={paramMap.configKey}>
        <td>{paramMap.configKey}</td>
        <td>{paramMap.paramName}</td>
        <td><Button bsStyle="info" bsSize="small" onClick={() => { this._parameterClear(paramMap.configKey); }}>Clear</Button></td>
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
    const typeRegExp = RegExp(/\.type$/);
    const configKeys = ObjectUtils.getPaths(this.props.entity.data)
      .filter(configKey => typeRegExp.test(configKey))
      .map((configKey) => { return configKey.replace(typeRegExp, ''); })
      .filter((configKey) => {
        return this.props.appliedParameter.findIndex((paramMap) => {
          return configKey === paramMap.configKey;
        }) < 0;
      });
    const emptyOption = (name) => { return (<option key="EMPTY" value="">{name}</option>); };
    const configOptions = [emptyOption('Choose Config Key')].concat(configKeys.map(key => <option key={key} value={key}>{key}</option>));
    let parameters = this.props.parameters;
    let emptyName = parameters.length <= 0 ? 'Create a parameter first' : 'Choose...';
    if (this.state.config_key !== '' && parameters.length > 0) {
      const configKeyType = ObjectUtils.getValue(this.props.entity.data, this.state.config_key).type;
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
            <DataTable
              id="config-key-list"
              headers={['Config Key', 'Parameter', 'Action']}
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
