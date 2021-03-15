import { ListField, NumberField, TextField } from 'components/configurationforms/types';

export const numberField: NumberField = {
  additional_info: {},
  attributes: [],
  default_value: 42,
  description: 'my number description',
  human_name: 'number field',
  is_optional: true,
  position: 100,
  type: 'number',
};

export const requiredNumberField: NumberField = {
  additional_info: {},
  attributes: [],
  default_value: 42,
  description: 'my number description',
  human_name: 'number field',
  is_optional: false,
  position: 100,
  type: 'number',
};

export const negativeNumberField: NumberField = {
  additional_info: {},
  attributes: ['only_negative'],
  default_value: -42,
  description: 'my number description',
  human_name: 'number field',
  is_optional: true,
  position: 100,
  type: 'number',
};

export const positiveNumberField: NumberField = {
  additional_info: {},
  attributes: ['only_positive'],
  default_value: 42,
  description: 'my number description',
  human_name: 'number field',
  is_optional: true,
  position: 100,
  type: 'number',
};

export const portNumberField: NumberField = {
  additional_info: {},
  attributes: ['is_port_number'],
  default_value: 42,
  description: 'my number description',
  human_name: 'number field',
  is_optional: true,
  position: 100,
  type: 'number',
};

export const textField: TextField = {
  human_name: 'text field',
  additional_info: {},
  description: 'my text description',
  default_value: 'foobar',
  attributes: [],
  position: 100,
  type: 'text',
  is_optional: true,
};

export const requiredTextField: TextField = {
  human_name: 'text field',
  additional_info: {},
  description: 'my text description',
  default_value: 'foobar',
  attributes: [],
  position: 100,
  type: 'text',
  is_optional: false,
};

export const passwordTextField: TextField = {
  human_name: 'password field',
  additional_info: {},
  description: 'my password description',
  default_value: 'secret',
  attributes: ['is_password'],
  position: 100,
  type: 'text',
  is_optional: true,
};

export const textAreaField: TextField = {
  human_name: 'text area field',
  additional_info: {},
  description: 'my long text description',
  default_value: 'hang in there',
  attributes: ['textarea'],
  position: 100,
  type: 'text',
  is_optional: true,
};

export const listField: ListField = {
  human_name: 'list field',
  additional_info: {
    values: {
      uno: 'one',
      dos: 'two',
    },
  },
  description: 'my list description',
  default_value: [],
  attributes: [],
  position: 100,
  type: 'list',
  is_optional: true,
};

export const creatableListField: ListField = {
  human_name: 'list field',
  additional_info: {
    values: {},
  },
  description: 'my list description',
  default_value: [],
  attributes: ['allow_create'],
  position: 100,
  type: 'list',
  is_optional: false,
};
