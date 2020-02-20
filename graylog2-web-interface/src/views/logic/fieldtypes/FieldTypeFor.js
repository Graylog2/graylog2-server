// @flow strict
import Series, { isFunction } from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';

import inferTypeForSeries from './InferTypeForSeries';
import FieldType from './FieldType';

const fieldTypeFor = (field: string, types: FieldTypeMappingsList): FieldType => {
  if (isFunction(field)) {
    const { type } = inferTypeForSeries(Series.forFunction(field), types);
    return type;
  }
  const fieldType = types.find(f => f.name === field);
  return fieldType ? fieldType.type : FieldType.Unknown;
};

export default fieldTypeFor;
