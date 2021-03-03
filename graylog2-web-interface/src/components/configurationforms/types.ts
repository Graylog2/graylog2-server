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
type NumberFieldAttributes = 'only_negative' | 'only_positive' | 'is_port_number';
type TextFieldAttributes = 'is_password' | 'textarea';
type ListFieldAttributes = 'allow_create';

export type NumberField = {
  additional_info: {},
  attributes: Array<NumberFieldAttributes>,
  default_value: number,
  description: string,
  human_name: string,
  is_optional: boolean,
  position: number,
  type: 'number',
};

export type TextField = {
  additional_info: {},
  attributes: Array<TextFieldAttributes>,
  default_value: string,
  description: string,
  human_name: string,
  is_optional: boolean,
  position: number,
  type: 'text',
};

export type ListField = {
  additional_info: {
    values: {
      [key: string]: string,
    }
  },
  attributes: Array<ListFieldAttributes>,
  default_value: Array<string>,
  description: string,
  human_name: string,
  is_optional: boolean,
  position: number,
  type: 'list',
};

export type DropdownField = {
  additional_info: {
    values: {
      [value: string]: string,
    }
  },
  attributes: [],
  default_value: string,
  description: string,
  human_name: string,
  is_optional: boolean,
  position: number,
  type: 'dropdown',
};

export type ConfigurationField = DropdownField | ListField | NumberField | TextField;
