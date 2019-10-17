import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';
import Field from 'views/components/Field';

import { useFieldList } from './FieldListContext';
import FieldTypeIcon from './FieldTypeIcon';

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

  return (
    <Wrap>
      <List>
        {fieldList.map(({ name, type }) => {
          const disabled = !fields.find(f => f.name === name);

          return (
            <Item>
              <FieldTypeIcon type={type} />

              <Field queryId={selectedQuery}
                     viewId={selectedView}
                     disabled={disabled}
                     name={name}
                     type={type}
                     interactive>
                {name}
              </Field>
            </Item>
          );
        })}
      </List>
    </Wrap>
  );
};

const Wrap = styled.div`
  overflow: auto;
`;

const List = styled.ul`
  list-style: none;
  margin: 0;
  padding: 0;
`;

const Item = styled.li`
  font-size: 12px;
  padding: 3px 0;
  display: table-row;
`;

FieldListWrap.propTypes = {
  fields: PropTypes.object.isRequired,
  allFields: PropTypes.object.isRequired,
  viewMetadata: PropTypes.object.isRequired,
};

export default FieldListWrap;
