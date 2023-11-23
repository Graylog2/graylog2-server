import type { Attributes } from 'stores/PaginationTypes';

export const customFiled = {
  id: 'field',
  fieldName: 'field',
  type: 'bool',
  isCustom: true,
  isReserved: false,
};

export const secondCustomFiled = {
  id: 'field-2',
  fieldName: 'field-2',
  type: 'bool',
  isCustom: true,
  isReserved: false,
};
export const reservedFiled = {
  id: 'field',
  fieldName: 'field',
  type: 'bool',
  isCustom: false,
  isReserved: true,
};

export const defaultFiled = {
  id: 'field',
  fieldName: 'field',
  type: 'bool',
  isCustom: false,
  isReserved: false,
};

export const attributes: Attributes = [
  {
    id: 'field_name',
    title: 'Field Name',
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'is_custom',
    title: 'Custom',
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'is_reserved',
    title: 'Reserved',
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'type',
    title: 'Type',
    type: 'STRING',
    sortable: true,
  },
];
