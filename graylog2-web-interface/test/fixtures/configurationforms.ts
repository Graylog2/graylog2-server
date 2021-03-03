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
import { DropdownField, ListField, NumberField, TextField } from 'components/configurationforms/types';

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

export const dropdownField: DropdownField = {
  human_name: 'dropdown field',
  additional_info: {
    values: {
      uno: 'one',
      dos: 'two',
    },
  },
  description: 'my dropdown description',
  default_value: '',
  attributes: [],
  position: 100,
  type: 'dropdown',
  is_optional: true,
};

export const requiredDropdownField: DropdownField = {
  human_name: 'required dropdown field',
  additional_info: {
    values: {},
  },
  description: 'my required dropdown description',
  default_value: '',
  attributes: [],
  position: 100,
  type: 'dropdown',
  is_optional: false,
};
