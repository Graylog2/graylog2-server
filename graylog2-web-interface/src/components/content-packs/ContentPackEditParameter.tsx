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

import i18n from 'i18n';
import { Input } from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';
import ObjectUtils from 'util/ObjectUtils';

import ContentPackUtils from './ContentPackUtils';

const t = (key: string) => i18n.t(key, { ns: 'content_packs' });

type ContentPackEditParameterProps = {
  onUpdateParameter?: (...args: any[]) => void;
  parameters?: any[];
  parameterToEdit?: any;
};

class ContentPackEditParameter extends React.Component<
  ContentPackEditParameterProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    onUpdateParameter: () => {},
    parameters: [],
    parameterToEdit: undefined,
  };

  constructor(props: ContentPackEditParameterProps) {
    super(props);

    this.state = {
      newParameter: props.parameterToEdit || ObjectUtils.clone(ContentPackEditParameter.emptyParameter),
      defaultValueError: undefined,
      nameError: undefined,
      titleError: undefined,
      descrError: undefined,
    };
  }

  static emptyParameter = {
    name: '',
    title: '',
    description: '',
    type: 'string',
    default_value: '',
  };

  private titleInput: Input;

  addNewParameter = (e: React.FormEvent<HTMLFormElement>) => {
    if (e) {
      e.preventDefault();
    }

    if (!this._validateParameter()) {
      return;
    }

    const realDefaultValue = ContentPackUtils.convertValue(
      this.state.newParameter.type,
      this.state.newParameter.default_value,
    );
    const updatedParameter = ObjectUtils.clone(this.state.newParameter);

    updatedParameter.default_value = realDefaultValue;
    this.props.onUpdateParameter(updatedParameter);

    this.titleInput.getInputDOMNode().focus();
    this.setState({ newParameter: ObjectUtils.clone(ContentPackEditParameter.emptyParameter) });
  };

  _updateField = (name: string, value: any) => {
    this.setState((parameter) => ({ newParameter: { ...parameter.newParameter, [name]: value } }));
  };

  _bindValue = (event: React.ChangeEvent<HTMLInputElement>) => {
    this._updateField(event.target.name, FormsUtils.getValueFromInput(event.target));
  };

  _validateName = () => {
    const value = this.state.newParameter.name;

    if (value.match(/\W/)) {
      this.setState({ nameError: t('edit_parameter.name_invalid') });

      return false;
    }

    if (
      (this.props.parameterToEdit || {}).name !== value &&
      this.props.parameters.findIndex((parameter) => parameter.name === value) >= 0
    ) {
      this.setState({ nameError: t('edit_parameter.name_not_unique') });

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
            this.setState({ defaultValueError: t('edit_parameter.not_integer') });

            return false;
          }

          break;
        }

        case 'double': {
          if (Number.isNaN(Number.parseFloat(value))) {
            this.setState({ defaultValueError: t('edit_parameter.not_double') });

            return false;
          }

          break;
        }

        case 'boolean': {
          if (value !== 'true' && value !== 'false') {
            this.setState({ defaultValueError: t('edit_parameter.not_boolean') });

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

  _validateParameter() {
    const param = this.state.newParameter;

    if (!param.name) {
      this.setState({ nameError: t('edit_parameter.name_required') });

      return false;
    }

    this.setState({ nameError: undefined });

    if (!param.title) {
      this.setState({ titleError: t('edit_parameter.title_required') });

      return false;
    }

    this.setState({ titleError: undefined });

    if (!param.description) {
      this.setState({ descrError: t('edit_parameter.description_required') });

      return false;
    }

    this.setState({ descrError: undefined });

    return this._validateDefaultValue() && this._validateName();
  }

  render() {
    const header = this.props.parameterToEdit ? t('edit_parameter.edit_title') : t('edit_parameter.create_title');
    const disableType = !!this.props.parameterToEdit;

    return (
      <div>
        <h2>{header}</h2>
        <br />
        <form
          className="parameter-form"
          id="parameter-form"
          data-testid="parameter-form"
          onSubmit={this.addNewParameter}>
          <fieldset>
            <Input
              ref={(node) => {
                this.titleInput = node;
              }}
              name="title"
              id="title"
              type="text"
              maxLength={250}
              value={this.state.newParameter.title}
              onChange={this._bindValue}
              bsStyle={this.state.titleError ? 'error' : null}
              label={t('edit_parameter.title')}
              help={this.state.titleError ? this.state.titleError : t('edit_parameter.title_help')}
              required
            />
            <Input
              name="name"
              id="name"
              type="text"
              maxLength={250}
              bsStyle={this.state.nameError ? 'error' : null}
              value={this.state.newParameter.name}
              onChange={this._bindValue}
              label={t('edit_parameter.name')}
              help={this.state.nameError ? this.state.nameError : t('edit_parameter.name_help')}
              required
            />
            <Input
              name="description"
              id="description"
              type="text"
              bsStyle={this.state.descrError ? 'error' : null}
              maxLength={250}
              value={this.state.newParameter.description}
              onChange={this._bindValue}
              label={t('edit_parameter.description')}
              help={this.state.descrError ? this.state.descrError : t('edit_parameter.description_help')}
              required
            />
            <Input
              name="type"
              id="type"
              type="select"
              disabled={disableType}
              value={this.state.newParameter.type}
              onChange={this._bindValue}
              label={t('edit_parameter.value_type')}
              help={t('edit_parameter.value_type_help')}
              required>
              <option value="string">{t('value_types.string')}</option>
              <option value="integer">{t('value_types.integer')}</option>
              <option value="double">{t('value_types.double')}</option>
              <option value="boolean">{t('value_types.boolean')}</option>
            </Input>
            <Input
              name="default_value"
              id="default_value"
              type="text"
              maxLength={250}
              bsStyle={this.state.defaultValueError ? 'error' : null}
              value={this.state.newParameter.default_value}
              onChange={this._bindValue}
              label={t('edit_parameter.default_value')}
              help={
                this.state.defaultValueError ? this.state.defaultValueError : t('edit_parameter.default_value_help')
              }
            />
          </fieldset>
        </form>
      </div>
    );
  }
}

export default ContentPackEditParameter;
