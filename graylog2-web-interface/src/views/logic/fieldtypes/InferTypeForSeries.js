// @flow strict
import Series, { parseSeries } from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';

import FieldType, { FieldTypes } from './FieldType';
import FieldTypeMapping from './FieldTypeMapping';

const typePreservingFunctions = ['avg', 'min', 'max', 'percentile'];
const constantTypeFunctions = {
  card: FieldTypes.LONG,
  count: FieldTypes.LONG,
};

const inferTypeForSeries = (series: Series, types: (FieldTypeMappingsList | Array<FieldTypeMapping>)): FieldTypeMapping => {
  const definition = parseSeries(series.function);
  const newMapping = (type) => FieldTypeMapping.create(series.function, type);
  if (definition === null) {
    return newMapping(FieldType.Unknown);
  }

  const { type, field } = definition;

  // $FlowFixMe: this check should...
  if (constantTypeFunctions[type]) {
    // $FlowFixMe: ... guard this access!
    return newMapping(constantTypeFunctions[type]());
  }

  if (typePreservingFunctions.includes(type)) {
    const mapping = types && types.find((t) => (t.name === field));

    if (!mapping) {
      return newMapping(FieldType.Unknown);
    }
    return newMapping(mapping.type);
  }

  return newMapping(FieldTypes.FLOAT());
};

export default inferTypeForSeries;
