import PropTypes from 'prop-types';
import React from 'react';

import { Input } from 'components/bootstrap';

import FormsUtils from 'util/FormsUtils';
import ObjectUtils from 'util/ObjectUtils';
import ContentPackUtils from './ContentPackUtils';

class ContentPackEditParameter extends React.Component {
  static propTypes = {
    onUpdateParameter: PropTypes.func,
    parameters: PropTypes.array,
    parameterToEdit: PropTypes.object,
  };

  static defaultProps = {
    onUpdateParameter: () => { },
    parameters: [],
    parameterToEdit: undefined,
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
      newParameter: props.parameterToEdit || ObjectUtils.clone(ContentPackEditParameter.emptyParameter),
      defaultValueError: undefined,
      nameError: undefined,
      titleError: undefined,
      descrError: undefined,
    };
  }

  addNewParameter = (e) => {
    if (e) {
      e.preventDefault();
    }

    if (!this._validateParameter()) {
      return;
    }

    const { newParameter } = this.state;
    const { onUpdateParameter } = this.props;
    const realDefaultValue = ContentPackUtils.convertValue(newParameter.type,
      newParameter.default_value);
    const updatedParameter = ObjectUtils.clone(newParameter);
    updatedParameter.default_value = realDefaultValue;
    onUpdateParameter(updatedParameter);

    this.titleInput.getInputDOMNode().focus();
    this.setState({ newParameter: ObjectUtils.clone(ContentPackEditParameter.emptyParameter) });
  };

  _updateField = (name, value) => {
    const { newParameter } = this.state;
    const updatedParameter = ObjectUtils.clone(newParameter);
    updatedParameter[name] = value;
    this.setState({ newParameter: updatedParameter });
  };

  _bindValue = (event) => {
    this._updateField(event.target.name, FormsUtils.getValueFromInput(event.target));
  };

  _validateParameter = () => {
    const { newParameter } = this.state;
    const param = newParameter;
    if (!param.name) {
      this.setState({ nameError: 'Name must be set.' });
      return false;
    }
    this.setState({ nameError: undefined });

    if (!param.title) {
      this.setState({ titleError: 'Title must be set.' });
      return false;
    }
    this.setState({ titleError: undefined });

    if (!param.description) {
      this.setState({ descrError: 'Description must be set.' });
      return false;
    }
    this.setState({ descrError: undefined });

    return this._validateDefaultValue() && this._validateName();
  };

  _validateName = () => {
    const { newParameter } = this.state;
    const { parameterToEdit = {}, parameters } = this.props;
    const value = newParameter.name;
    if (value.match(/\W/)) {
      this.setState({ nameError: 'The parameter name must only contain A-Z, a-z, 0-9 and _' });
      return false;
    }

    if (parameterToEdit.name !== value
      && parameters.findIndex((parameter) => { return parameter.name === value; }) >= 0) {
      this.setState({ nameError: 'The parameter name must be unique.' });
      return false;
    }

    this.setState({ nameError: undefined });
    return true;
  };

  _validateDefaultValue = () => {
    const { newParameter } = this.state;
    const value = newParameter.default_value;
    if (value) {
      switch (newParameter.type) {
        case 'integer': {
          if (`${parseInt(value, 10)}` !== value) {
            this.setState({ defaultValueError: 'This is not an integer value.' });
            return false;
          }
          break;
        }
        case 'double': {
          if (Number.isNaN(Number(value))) {
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
    const { parameterToEdit } = this.props;
    const header = parameterToEdit ? 'Edit parameter' : 'Create parameter';
    const disableType = !!parameterToEdit;
    const { defaultValueError, nameError, descrError, newParameter, titleError } = this.state;
    return (
      <div>
        <h2>{header}</h2>
        <br />
        <form className="parameter-form" id="parameter-form" onSubmit={this.addNewParameter}>
          <fieldset>
            <Input ref={(node) => { this.titleInput = node; }}
                   name="title"
                   id="title"
                   type="text"
                   maxLength={250}
                   value={newParameter.title}
                   onChange={this._bindValue}
                   bsStyle={titleError ? 'error' : null}
                   label="Title"
                   help={titleError || 'Give a descriptive title for this content pack.'}
                   required />
            <Input name="name"
                   id="name"
                   type="text"
                   maxLength={250}
                   bsStyle={nameError ? 'error' : null}
                   value={newParameter.name}
                   onChange={this._bindValue}
                   label="Name"
                   help={nameError || 'This is used as the parameter reference and must not contain a space.'}
                   required />
            <Input name="description"
                   id="description"
                   type="text"
                   bsStyle={descrError ? 'error' : null}
                   maxLength={250}
                   value={newParameter.description}
                   onChange={this._bindValue}
                   label="Description"
                   help={descrError || 'Give a description explaining what will be done with this parameter.'}
                   required />
            <Input name="type"
                   id="type"
                   type="select"
                   disabled={disableType}
                   value={newParameter.type}
                   onChange={this._bindValue}
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
                   bsStyle={defaultValueError ? 'error' : null}
                   value={newParameter.default_value}
                   onChange={this._bindValue}
                   label="Default value"
                   help={defaultValueError || 'Give a default value if the parameter is not optional.'} />
          </fieldset>
          <button style={{ display: 'none' }} type="submit" />
        </form>
      </div>
    );
  }
}

export default ContentPackEditParameter;
