import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';

import pivotForField from 'enterprise/logic/searchtypes/aggregation/PivotGenerator';
import Pivot from 'enterprise/logic/aggregationbuilder/Pivot';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import { FieldTypesStore } from 'enterprise/stores/FieldTypesStore';
import ConfigurableElement from './ConfigurableElement';
import SortableSelect from './SortableSelect';
import PivotConfiguration from './PivotConfiguration';
import CustomPropTypes from '../CustomPropTypes';
import FieldTypeMapping from '../../logic/fieldtypes/FieldTypeMapping';

const _onChange = (fields, newValue, onChange, fieldTypes) => {
  const newFields = newValue.map(v => v.value);

  return onChange(newFields.map((field) => {
    const existingField = fields.find(f => f.field === field);
    const mapping = fieldTypes.find(fieldType => fieldType.name === field) || new FieldTypeMapping(field, FieldType.Unknown);
    return existingField || pivotForField(field, mapping.type);
  }));
};

const configFor = ({ value }, values) => (value === '' ? {} : values.find(({ field }) => field === value).config);
const newPivotConfigChange = (values, value, newPivotConfig, onChange) => {
  const newValues = values.map((pivot) => {
    if (pivot.field === value.value) {
      return new Pivot(pivot.field, pivot.type, newPivotConfig.config);
    }
    return pivot;
  });
  return onChange(newValues);
};

const PivotSelect = ({ onChange, fields, value, ...props }) => {
  // eslint-disable-next-line react/prop-types
  const valueComponent = ({ children, ...rest }) => {
    const element = rest.value;
    const fieldTypes = fields.all.filter(v => v.name === element.label);
    const fieldType = fieldTypes.isEmpty() ? FieldType.Unknown : fieldTypes.first().type;
    return (
      <ConfigurableElement {...rest}
                           configuration={({ onClose }) => <PivotConfiguration type={fieldType} config={configFor(element, value)} onClose={onClose} />}
                           onChange={newPivotConfig => newPivotConfigChange(value, element, newPivotConfig, onChange)}
                           title="Pivot Configuration">
        {children}
      </ConfigurableElement>
    );
  };

  return <SortableSelect {...props} onChange={newValue => _onChange(value, newValue, onChange, fields.all)} value={value} valueComponent={valueComponent} />;
};

PivotSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  fields: PropTypes.shape({
    all: CustomPropTypes.FieldListType,
  }).isRequired,
  value: PropTypes.arrayOf(PropTypes.object).isRequired,
};

export default connect(PivotSelect, { fields: FieldTypesStore });
