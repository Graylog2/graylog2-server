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
import PropTypes from 'prop-types';
import { debounce, cloneDeep } from 'lodash';
import React from 'react';

import { Button } from 'components/graylog';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import CombinedProvider from 'injection/CombinedProvider';

import ConfigurationHelperStyle from './ConfigurationHelper.css';

const { ConfigurationVariableActions } = CombinedProvider.get('ConfigurationVariable');

class EditConfigurationVariableModal extends React.Component {
  static propTypes = {
    create: PropTypes.bool,
    id: PropTypes.string,
    name: PropTypes.string,
    description: PropTypes.string,
    content: PropTypes.string,
    saveConfigurationVariable: PropTypes.func.isRequired,
  };

  static defaultProps = {
    create: false,
    id: '',
    name: '',
    description: '',
    content: '',
  };

  constructor(props) {
    super(props);
    this.state = this._cleanState();
  }

  _cleanState = () => {
    const { name, id, description, content } = this.props;

    return {
      error: false,
      validation_errors: {},
      savedName: name,
      formData: {
        id,
        name,
        description,
        content,
      },
    };
  };

  _hasErrors = () => {
    const { error } = this.state;

    return error;
  };

  openModal = () => {
    this.setState(this._cleanState());
    this.modal.open();
  };

  _getId = (prefixIdName) => {
    const { id } = this.state;

    return `${prefixIdName} ${id}` || 'new';
  };

  _saved = () => {
    this.modal.close();
  };

  _validateFormData = (nextFormData) => {
    ConfigurationVariableActions.validate(nextFormData).then((validation) => {
      this.setState({ validation_errors: validation.errors, error: validation.failed });
    });
  };

  // Needs to be after _validateFormData is defined
  // eslint-disable-next-line react/sort-comp
  _debouncedValidateFormData = debounce(this._validateFormData, 200);

  _formDataUpdate = (key) => {
    const { formData } = this.state;

    return (nextValue) => {
      const nextFormData = cloneDeep(formData);

      nextFormData[key] = nextValue;
      this._debouncedValidateFormData(nextFormData);
      this.setState({ formData: nextFormData });
    };
  };

  _save = () => {
    const { formData, savedName } = this.state;
    const { saveConfigurationVariable } = this.props;

    if (this._hasErrors()) {
      // Ensure we display an error on the content field, as this is not validated by the browser
      this._validateFormData(formData);

      return;
    }

    saveConfigurationVariable(formData, savedName, this._saved);
  };

  _handleInputChange = (event) => {
    this._formDataUpdate(event.target.name)(event.target.value);
  };

  _formatValidationMessage = (fieldName, defaultText) => {
    const { validation_errors: validationErrors } = this.state;

    if (validationErrors[fieldName]) {
      return <span>{validationErrors[fieldName][0]}</span>;
    }

    return <span>{defaultText}</span>;
  };

  _validationState = (fieldName) => {
    const { validation_errors: validationErrors } = this.state;

    if (validationErrors[fieldName]) {
      return 'error';
    }

    return null;
  };

  render() {
    const { create } = this.props;
    const { formData } = this.state;

    let triggerButtonContent;

    if (create) {
      triggerButtonContent = 'Create Variable';
    } else {
      triggerButtonContent = <span>Edit</span>;
    }

    return (
      <>
        <Button onClick={this.openModal}
                bsStyle={create ? 'success' : 'info'}
                bsSize={create ? 'small' : 'xsmall'}
                className={create ? 'pull-right' : ''}>
          {triggerButtonContent}
        </Button>
        <BootstrapModalForm ref={(ref) => { this.modal = ref; }}
                            title={<>{create ? 'Create' : 'Edit'} Variable $&#123;user.{formData.name}&#125;</>}
                            onSubmitForm={this._save}
                            onModalClose={this._cleanState}
                            submitButtonDisabled={this._hasErrors()}
                            submitButtonText="Save">
          <fieldset>
            <Input type="text"
                   id={this._getId('variable-name')}
                   label="Name"
                   name="name"
                   defaultValue={formData.name}
                   onChange={this._handleInputChange}
                   bsStyle={this._validationState('name')}
                   help={this._formatValidationMessage('name', 'Type a name for this variable')}
                   autoFocus
                   spellCheck={false}
                   required />
            <Input type="text"
                   id={this._getId('variable-description')}
                   label={<span>Description <small className="text-muted">(Optional)</small></span>}
                   name="description"
                   defaultValue={formData.description}
                   onChange={this._handleInputChange}
                   help="Type a description for this variable"
                   spellCheck={false} />
            <Input type="textarea"
                   id={this._getId('variable-content')}
                   label="Content"
                   name="content"
                   rows="10"
                   className={ConfigurationHelperStyle.monoSpaceFont}
                   defaultValue={formData.content}
                   onChange={this._handleInputChange}
                   bsStyle={this._validationState('content')}
                   help={this._formatValidationMessage('content', 'Write your variable content')}
                   spellCheck={false}
                   required />
          </fieldset>
        </BootstrapModalForm>
      </>
    );
  }
}

export default EditConfigurationVariableModal;
