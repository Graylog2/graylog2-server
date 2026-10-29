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
import debounce from 'lodash/debounce';
import cloneDeep from 'lodash/cloneDeep';
import React from 'react';
import { withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import { Button, BootstrapModalForm, Input } from 'components/bootstrap';
import { validateConfigurationVariable } from 'api/configuration-variables';

import ConfigurationHelperStyle from './ConfigurationHelper.css';

type EditConfigurationVariableModalProps = {
  create?: boolean;
  id?: string;
  name?: string;
  description?: string;
  content?: string;
  saveConfigurationVariable: (...args: any[]) => void;
} & WithTranslation;

class EditConfigurationVariableModal extends React.Component<
  EditConfigurationVariableModalProps,
  {
    [key: string]: any;
  }
> {
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
      showModal: false,
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
    this.setState({ ...this._cleanState(), showModal: true });
  };

  closeModal = () => {
    this.setState(this._cleanState());
  };

  _getId = (prefixIdName) => {
    const { id } = this.state;

    return `${prefixIdName} ${id || 'new'}`;
  };

  _saved = () => {
    this.setState({ showModal: false });
  };

  _validateFormData = (nextFormData) => {
    validateConfigurationVariable(nextFormData).then((validation: any) => {
      this.setState({ validation_errors: validation.errors, error: validation.failed });
    });
  };

  // Needs to be after _validateFormData is defined

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
      return 'error' as const;
    }

    return null;
  };

  render() {
    const { create, t } = this.props;
    const { formData, showModal } = this.state;

    let triggerButtonContent;

    if (create) {
      triggerButtonContent = t('configuration_forms.edit_configuration_variable_modal.create_variable');
    } else {
      triggerButtonContent = <span>{t('configuration_forms.edit_configuration_variable_modal.edit')}</span>;
    }

    return (
      <>
        <Button
          onClick={this.openModal}
          bsStyle={create ? 'success' : 'info'}
          bsSize={create ? 'small' : 'xsmall'}
          className={create ? 'pull-right' : ''}>
          {triggerButtonContent}
        </Button>
        <BootstrapModalForm
          show={showModal}
          title={t('configuration_forms.edit_configuration_variable_modal.title', {
            action: create
              ? t('configuration_forms.edit_configuration_variable_modal.create_action')
              : t('configuration_forms.edit_configuration_variable_modal.edit_action'),
            name: formData.name,
          })}
          data-telemetry-title={`${create ? 'Create' : 'Edit'} Variable user`}
          onSubmitForm={this._save}
          onCancel={this.closeModal}
          submitButtonDisabled={this._hasErrors()}
          submitButtonText={t('common.save')}>
          <fieldset>
            <Input
              type="text"
              id={this._getId('variable-name')}
              label={t('configuration_forms.edit_configuration_variable_modal.name')}
              name="name"
              defaultValue={formData.name}
              onChange={this._handleInputChange}
              bsStyle={this._validationState('name')}
              help={this._formatValidationMessage(
                'name',
                t('configuration_forms.edit_configuration_variable_modal.name_help'),
              )}
              autoFocus
              spellCheck={false}
              required
            />
            <Input
              type="text"
              id={this._getId('variable-description')}
              label={
                <span>
                  {t('configuration_forms.edit_configuration_variable_modal.description')}{' '}
                  <small className="text-muted">
                    {t('configuration_forms.edit_configuration_variable_modal.optional')}
                  </small>
                </span>
              }
              name="description"
              defaultValue={formData.description}
              onChange={this._handleInputChange}
              help={t('configuration_forms.edit_configuration_variable_modal.description_help')}
              spellCheck={false}
            />
            <Input
              type="textarea"
              id={this._getId('variable-content')}
              label={t('configuration_forms.edit_configuration_variable_modal.content')}
              name="content"
              rows={10}
              className={ConfigurationHelperStyle.monoSpaceFont}
              defaultValue={formData.content}
              onChange={this._handleInputChange}
              bsStyle={this._validationState('content')}
              help={this._formatValidationMessage(
                'content',
                t('configuration_forms.edit_configuration_variable_modal.content_help'),
              )}
              spellCheck={false}
              required
            />
          </fieldset>
        </BootstrapModalForm>
      </>
    );
  }
}

export default withTranslation('sidecars')(EditConfigurationVariableModal);
