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

export type ConfigurationField = ListField | NumberField | TextField;
