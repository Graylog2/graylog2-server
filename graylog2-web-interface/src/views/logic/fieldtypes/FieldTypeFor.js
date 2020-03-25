// @flow strict
import Series, { isFunction } from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';

import inferTypeForSeries from './InferTypeForSeries';
import FieldType from './FieldType';
import FieldTypeMapping from './FieldTypeMapping';

const fieldTypeFor = (field: string, types: (FieldTypeMappingsList | Array<FieldTypeMapping>)): FieldType => {
  if (isFunction(field)) {
    const { type } = inferTypeForSeries(Series.forFunction(field), types);
    return type;
  }
  const fieldType = types && types.find(f => f.name === field);
  return fieldType ? fieldType.type : FieldType.Unknown;
};

export default fieldTypeFor;
