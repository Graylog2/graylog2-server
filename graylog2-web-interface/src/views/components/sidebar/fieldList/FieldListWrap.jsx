import React from 'react';
import PropTypes from 'prop-types';
import { FixedSizeList as List } from 'react-window';

import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';
import Field from 'views/components/Field';

import { useFieldList } from './FieldListContext';
import styles from './FieldList.css';

import FieldTypeIcon from '../FieldTypeIcon';

const isReservedField = fieldName => MessageFieldsFilter.FILTERED_FIELDS.includes(fieldName);

const FieldListWrap = ({ fields, allFields, viewMetadata }) => {
  const { filter, showFieldsBy } = useFieldList();

  const fieldsToShow = () => {
    const isNotReservedField = f => !isReservedField(f.name);

    switch (showFieldsBy) {
      case 'all':
        return allFields.filter(isNotReservedField);
      case 'allreserved':
        return allFields;
      case 'current':
      default:
        return fields.filter(isNotReservedField);
    }
  };

  const {
    id: selectedView,
    activeQuery: selectedQuery,
  } = viewMetadata;

  if (!fields) {
    return <span>No field information available.</span>;
  }
  const fieldFilter = filter ? (field => field.name.toLocaleUpperCase().includes(filter.toLocaleUpperCase())) : () => true;
  const fieldList = fieldsToShow().filter(fieldFilter).sortBy(field => field.name.toLocaleUpperCase());

  if (fieldList.isEmpty()) {
    return <i>No fields to show. Try changing your filter term or select a different field set above.</i>;
  }

  const FieldListRow = ({ fieldList, fields, selectedQuery, selectedView }) => ({ index, style }) => {
    return (
      <FieldItem fieldType={fieldList.get(index)}
                 selectedQuery={selectedQuery}
                 selectedView={selectedView}
                 fields={fields}
                 style={style} />
    );
  };

  const FieldItem = ({ fields, fieldType: { name, type }, selectedQuery, selectedView, style }) => {
    const disabled = !fields.find(f => f.name === name);

    return (
      <li key={`field-${name}`} className={styles.fieldListItem} style={style}>
        <FieldTypeIcon type={type} />
        {' '}
        <Field queryId={selectedQuery}
               viewId={selectedView}
               disabled={disabled}
               name={name}
               type={type}
               interactive>
          {name}
        </Field>
      </li>
    );
  };

  return (
    <div>
      <List height={50}
            itemCount={fieldList.size}
            itemSize={17}>
        {FieldListRow({ fieldList, fields, selectedQuery, selectedView })}
      </List>
    </div>
  );
};

FieldListWrap.propTypes = {
  fields: PropTypes.object.isRequired,
  allFields: PropTypes.object.isRequired,
  viewMetadata: PropTypes.object.isRequired,
};

export default FieldListWrap;
