// @flow strict

import { parseSeries } from 'enterprise/components/visualizations/Series';

import FieldType, { FieldTypes } from './FieldType';
import FieldTypeMapping from './FieldTypeMapping';
import Series from '../aggregationbuilder/Series';

const typePreservingFunctions = ['avg', 'min', 'max'];
const constantTypeFunctions = {
  card: FieldTypes.LONG,
  count: FieldTypes.LONG,
};

const inferTypeForSeries = (series: Series, types: Array<FieldTypeMapping>): FieldTypeMapping => {
  const definition = parseSeries(series.function);
  const newMapping = type => FieldTypeMapping.create(series.function, type);
  if (definition === null) {
    return newMapping(FieldType.Unknown);
  }

  const { type, field } = definition;

  if (constantTypeFunctions[type]) {
    return newMapping(constantTypeFunctions[type]());
  }

  if (typePreservingFunctions.includes(type)) {
    const mapping = types.find(t => (t.name === field));

    if (!mapping) {
      return newMapping(FieldType.Unknown);
    }
    return newMapping(mapping.type);
  }

  return newMapping(FieldTypes.FLOAT());
};

export default inferTypeForSeries;
