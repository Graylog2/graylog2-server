import { NumberField } from 'components/configurationforms/types';

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
  attributes: ['ONLY_NEGATIVE'],
  default_value: -42,
  description: 'my number description',
  human_name: 'number field',
  is_optional: true,
  position: 100,
  type: 'number',
};

export const positiveNumberField: NumberField = {
  additional_info: {},
  attributes: ['ONLY_POSITIVE'],
  default_value: 42,
  description: 'my number description',
  human_name: 'number field',
  is_optional: true,
  position: 100,
  type: 'number',
};

export const portNumberField: NumberField = {
  additional_info: {},
  attributes: ['IS_PORT_NUMBER'],
  default_value: 42,
  description: 'my number description',
  human_name: 'number field',
  is_optional: true,
  name: 'number_field',
  position: 100,
  title: 'number field title',
  type: 'number',
};
