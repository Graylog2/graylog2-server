// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';

import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

import ConfigurableElement from './ConfigurableElement';
import SortableSelect from './SortableSelect';
import PivotConfiguration from './PivotConfiguration';

const _onChange = (fields, newValue, onChange, fieldTypes) => {
  const newFields = newValue.map((v) => v.value);

  return onChange(newFields.map((field) => {
    const existingField = fields.find((f) => f.field === field);
    const mapping = fieldTypes?.find((fieldType) => fieldType.name === field) || new FieldTypeMapping(field, FieldType.Unknown);

    return existingField || pivotForField(field, mapping.type);
  }));
};

const configFor = ({ value }, values) => (value === '' ? {} : values.find(({ field }) => field === value)?.config);

const newPivotConfigChange = (values, value, newPivotConfig, onChange) => {
  const newValues = values.map((pivot) => {
    if (pivot.field === value.value) {
      return new Pivot(pivot.field, pivot.type, newPivotConfig.config);
    }

    return pivot;
  });

  return onChange(newValues);
};

type Props = {
  onChange: Array<Pivot> => void,
  value: Array<Pivot>,
};

const PivotSelect = ({ onChange, value, ...props }: Props) => {
  const fieldTypes = useContext(FieldTypesContext);
  // eslint-disable-next-line react/prop-types
  const ValueComponent = ({ children, innerProps, ...rest }) => {
    const element = rest.data;
    const fields = fieldTypes?.all
      ? fieldTypes.all.filter((v) => v.name === element.label)
      : Immutable.List();
    const fieldType = fields.isEmpty() ? FieldType.Unknown : fields.first().type;
    // eslint-disable-next-line react/prop-types
    const { className } = innerProps;

    return (
      <span className={className}>
        <ConfigurableElement {...rest}
                             configuration={({ onClose }) => <PivotConfiguration type={fieldType} config={configFor(element, value)} onClose={onClose} />}
                             onChange={(newPivotConfig) => newPivotConfigChange(value, element, newPivotConfig, onChange)}
                             title="Pivot Configuration">
          {children}
        </ConfigurableElement>
      </span>
    );
  };

  return <SortableSelect {...props} onChange={(newValue) => _onChange(value, newValue, onChange, fieldTypes?.all)} value={value} valueComponent={ValueComponent} />;
};

PivotSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  value: PropTypes.arrayOf(PropTypes.object).isRequired,
};

export default PivotSelect;
