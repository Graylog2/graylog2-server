import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col, Button, Modal } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import DataTable from 'components/common/DataTable';
import FormsUtils from 'util/FormsUtils';
import ObjectUtils from 'util/ObjectUtils';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

import ContentPackApplyParameter from 'components/content-packs/ContentPackApplyParameter';

class ContentPackParameters extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    onStateChange: PropTypes.func,
    appliedParameter: PropTypes.object.isRequired,
  };

  static defaultProps = {
    onStateChange: () => {},
  };

  static emptyParameter = {
    name: '',
    title: '',
    description: '',
    type: 'string',
    default_value: '',
  };

  constructor(props) {
    super(props);
    this.state = {
      newParameter: ObjectUtils.clone(ContentPackParameters.emptyParameter),
      defaultValueError: undefined,
      nameError: undefined,
    };

    this._addNewParameter = this._addNewParameter.bind(this);
    this._bindValue = this._bindValue.bind(this);
  }

  _updateField(name, value) {
    const updatedParameter = ObjectUtils.clone(this.state.newParameter);
    updatedParameter[name] = value;
    this.setState({ newParameter: updatedParameter });
  }

  _bindValue(event) {
    this._updateField(event.target.name, FormsUtils.getValueFromInput(event.target));
  }

  _addNewParameter(e) {
    e.preventDefault();

    if (!this._validateParameter()) {
      return;
    }
    const newContentPack = ObjectUtils.clone(this.props.contentPack);
    newContentPack.parameters.push(this.state.newParameter);
    this.props.onStateChange({ contentPack: newContentPack });
    this.setState({ newParameter: ObjectUtils.clone(ContentPackParameters.emptyParameter) });
  }

  _onParameterApply = (id, configKey, paramName) => {
    const paramMap = { configKey: configKey, paramName: paramName };
    const newAppliedParameter = ObjectUtils.clone(this.props.appliedParameter);
    newAppliedParameter[id] = newAppliedParameter[id] || [];
    newAppliedParameter[id].push(paramMap);
    this.props.onStateChange({ appliedParameter: newAppliedParameter });
  };

  _onParameterClear = (id, configKey) => {
    const newAppliedParameter = ObjectUtils.clone(this.props.appliedParameter);
    const indexToRemove = newAppliedParameter[id].findIndex((paramMap) => { return paramMap.configKey === configKey; });
    newAppliedParameter[id].splice(indexToRemove, 1);
    this.props.onStateChange({ appliedParameter: newAppliedParameter });
  };

  _deleteParameter = (parameter) => {
    const newContentPack = ObjectUtils.clone(this.props.contentPack);
    const indexToDelete = newContentPack.parameters.map(p => p.name).indexOf(parameter.name);
    if (indexToDelete < 0) {
      return;
    }
    newContentPack.parameters.splice(indexToDelete, 1);
    this.props.onStateChange({ contentPack: newContentPack });
  };

  _parameterRowFormatter = (parameter) => {
    return (
      <tr key={parameter.title}>
        <td>{parameter.title}</td>
        <td>{parameter.name}</td>
        <td>{parameter.description}</td>
        <td>{parameter.type}</td>
        <td>{parameter.default_value}</td>
        <td><Button bsStyle="primary" bsSize="small" onClick={() => { this._deleteParameter(parameter); }}>Delete</Button></td>
      </tr>
    );
  };

  _entityRowFormatter = (entity) => {
    let modalRef;
    const applyParamComponent = (<ContentPackApplyParameter
      parameters={this.props.contentPack.parameters}
      entity={entity}
      appliedParameter={this.props.appliedParameter[entity.id]}
      onParameterApply={(key, value) => { this._onParameterApply(entity.id, key, value); }}
      onParameterClear={(key) => { this._onParameterClear(entity.id, key); }}
    />);

    const closeModal = () => {
      modalRef.close();
    };

    const open = () => {
      modalRef.open();
    };

    const modal = (
      <BootstrapModalWrapper ref={(node) => { modalRef = node; }} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>Apply Parameter</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {applyParamComponent}
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={closeModal}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );

    const disableBtn = this.props.contentPack.parameters.length <= 0;
    const appliedParameterCount = (this.props.appliedParameter[entity.id] || []).length;
    return (
      <tr key={entity.data.title}>
        <td>{entity.data.title}</td>
        <td>{entity.type}</td>
        <td>{entity.data.description || entity.data.name}</td>
        <td>{appliedParameterCount}</td>
        <td>
          <Button bsStyle="primary"
                  bsSize="small"
                  disabled={disableBtn}
                  onClick={() => { open(); }}>
            Apply Parameter
          </Button>
        </td>
        {modal}
      </tr>
    );
  };

  _validateParameter() {
    const param = this.state.newParameter;
    if (!param.name || !param.title || !param.description) {
      return false;
    }
    return this._validateDefaultValue() && this._validateName();
  }

  _validateName = () => {
    const value = this.state.newParameter.name;
    if (value.match(/\W/)) {
      this.setState({ nameError: 'The parameter name must only contain A-Z, a-z, 0-9 and _' });
      return false;
    }

    if (this.props.contentPack.parameters
      .findIndex((parameter) => { return parameter.name === value; }) >= 0) {
      this.setState({ nameError: 'The parameter name must be unique.' });
      return false;
    }

    this.setState({ nameError: undefined });
    return true;
  };

  _validateDefaultValue = () => {
    const value = this.state.newParameter.default_value;
    if (value) {
      switch (this.state.newParameter.type) {
        case 'integer': {
          if (`${parseInt(value, 10)}` !== value) {
            this.setState({ defaultValueError: 'This is not an integer value.' });
            return false;
          }
          break;
        }
        case 'double': {
          if (isNaN(value)) {
            this.setState({ defaultValueError: 'This is not a double value.' });
            return false;
          }
          break;
        }
        case 'boolean': {
          if (value !== 'true' && value !== 'false') {
            this.setState({ defaultValueError: 'This is not a boolean value. It must be either true or false.' });
            return false;
          }
          break;
        }
        default:
          break;
      }
    }
    this.setState({ defaultValueError: undefined });
    return true;
  };

  render() {
    return (
      <div>
        <Row>
          <Col lg={8}>
            <h2>Create parameters</h2>
            <br />
            <form className="form-horizontal parameter-form" id="parameter-form" onSubmit={this._addNewParameter}>
              <fieldset>
                <Input name="title"
                       id="title"
                       type="text"
                       maxLength={250}
                       value={this.state.newParameter.title}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Title"
                       help="Give a descriptive title for this content pack."
                       required />
                <Input name="name"
                       id="name"
                       type="text"
                       maxLength={250}
                       bsStyle={this.state.nameError ? 'error' : null}
                       value={this.state.newParameter.name}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Name"
                       help={this.state.nameError ? this.state.nameError :
                         'This is used as the parameter reference and must not contain a space.'}
                       required />
                <Input name="description"
                       id="description"
                       type="text"
                       maxLength={250}
                       value={this.state.newParameter.description}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Description"
                       help="Give a description explaining what will be done with this parameter."
                       required />
                <Input name="type"
                       id="type"
                       type="select"
                       value={this.state.newParameter.type}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Value Type"
                       help="Give the type of the parameter."
                       required>
                  <option value="string">String</option>
                  <option value="integer">Integer</option>
                  <option value="double">Double</option>
                  <option value="boolean">Boolean</option>
                </Input>
                <Input name="default_value"
                       id="default_value"
                       type="text"
                       maxLength={250}
                       bsStyle={this.state.defaultValueError ? 'error' : null}
                       value={this.state.newParameter.default_value}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Default value"
                       help={this.state.defaultValueError ? this.state.defaultValueError :
                         'Give a default value if the parameter is not optional.'} />
                <Row>
                  <Col smOffset={10}>
                    <Button bsStyle="info" type="submit">Add Parameter</Button>
                  </Col>
                </Row>
              </fieldset>
            </form>
          </Col>
        </Row>
        <Row>
          <Col smOffset={1} sm={7}>
            <h2>Parameters list</h2>
            <br />
            <DataTable
              id="parameter-list"
              headers={['Title', 'Name', 'Description', 'Value Type', 'Default Value', 'Action']}
              sortByKey="title"
              filterKeys={[]}
              rows={this.props.contentPack.parameters}
              dataRowFormatter={this._parameterRowFormatter}
            />
          </Col>
        </Row>
        <Row>
          <Col smOffset={1} sm={9}>
            <h2>Entity list</h2>
            <br />
            <DataTable
              id="entity-list"
              headers={['Title', 'Type', 'Description', 'Applied Parameter', 'Action']}
              sortByKey="type"
              filterKeys={[]}
              rows={this.props.contentPack.entities}
              dataRowFormatter={this._entityRowFormatter}
            />
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackParameters;
