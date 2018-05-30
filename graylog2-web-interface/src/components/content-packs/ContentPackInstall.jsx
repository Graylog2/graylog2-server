import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col } from 'react-bootstrap';
import { Input } from 'components/bootstrap';

import ContentPackEntitiesList from './ContentPackEntitiesList';

class ContentPackInstall extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    onInstall: PropTypes.func,
  };

  static defaultProps = {
    onInstall: () => {},
  };

  static _convertToString(parameter) {
    switch (parameter.type) {
      case 'integer':
      case 'double':
        return parameter.default_value.toString();
      case 'boolean':
        return parameter.default_value ? 'true' : 'false';
      default:
        return parameter.default_value;
    }
  }

  constructor(props) {
    super(props);

    const parameterInput = props.contentPack.parameters.reduce((result, parameter) => {
      if (parameter.default_value) {
        const newResult = result;
        newResult[parameter.name] = ContentPackInstall._convertToString(parameter);
        return newResult;
      }
      return result;
    }, {});

    this.state = {
      parameterInput: parameterInput,
      errorMessages: {},
    };
  }

  onInstall = () => {
    if (this._validateInput()) {
      const contentPackId = this.props.contentPack.id;
      const contentPackRev = this.props.contentPack.rev;
      this.props.onInstall(contentPackId, contentPackRev, this.state.parameterInput);
    }
  };

  _getValue = (name, value) => {
    const newParameterInput = this.state.parameterInput;
    newParameterInput[name] = value;
    this.setState({ parameterInput: newParameterInput });
  };

  _validateInput = () => {
    const { parameterInput } = this.state;
    const errors = this.props.contentPack.parameters.reduce((result, parameter) => {
      if (parameterInput[parameter.name] && parameterInput[parameter.name].length > 0) {
        return result;
      }
      const newResult = result;
      newResult[parameter.name] = 'Needs to be filled.';
      return newResult;
    }, {});
    this.setState({ errorMessages: errors });
    return Object.keys(errors).length <= 0;
  };

  renderParameter(parameter) {
    const error = this.state.errorMessages[parameter.name];
    return (
      <Input name={parameter.name}
             id={parameter.name}
             key={parameter.name}
             type="text"
             maxLength={250}
             value={this.state.parameterInput[parameter.name] || ''}
             onChange={(e) => { this._getValue(parameter.name, e.target.value); }}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-7"
             label={parameter.title}
             help={error || parameter.description}
             bsStyle={error ? 'error' : undefined}
             required />
    );
  }

  render() {
    const parameterInput = this.props.contentPack.parameters.map((parameter) => {
      return this.renderParameter(parameter);
    });
    return (<div>
      {parameterInput.length > 0 &&
      <Row>
        <Col smOffset={1}>
          <h2>Configure Parameter</h2>
          <br />
          <br />
          {parameterInput}
        </Col>
      </Row>}
      <Row>
        <Col smOffset={1} sm={10}>
          <ContentPackEntitiesList contentPack={this.props.contentPack} readOnly />
        </Col>
      </Row>
    </div>);
  }
}

export default ContentPackInstall;
