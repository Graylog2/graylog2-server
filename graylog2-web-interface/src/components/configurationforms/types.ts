type NumberFieldAttributes = 'ONLY_NEGATIVE' | 'ONLY_POSITIVE' | 'IS_PORT_NUMBER';

export type NumberField = {
  additional_info: {},
  attributes: Array<NumberFieldAttributes>,
  default_value: number,
  description: string,
  human_name: string,
  is_optional: boolean,
  name: string,
  position: number,
  title: string,
  type: 'number',
};

export type ConfigurationField = NumberField;
