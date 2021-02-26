type NumberFieldAttributes = 'only_negative' | 'only_positive' | 'is_port_number';

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

export type ConfigurationField = NumberField;
