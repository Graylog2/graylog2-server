import React, { useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { FixedSizeList as List } from 'react-window';

import connect from 'stores/connect';
import { Button } from 'components/graylog';
import Field from 'views/components/Field';
import FieldTypeIcon from 'views/components/sidebar/FieldTypeIcon';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';

import styles from './FieldList.css';

const isReservedField = fieldName => MessageFieldsFilter.FILTERED_FIELDS.includes(fieldName);

export const FieldItem = ({ fields, fieldType: { name, type }, selectedQuery, selectedView, style }) => {
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

FieldItem.propTypes = {
  fields: PropTypes.object.isRequired,
  fieldType: PropTypes.object.isRequired,
  selectedQuery: PropTypes.string.isRequired,
  selectedView: PropTypes.string,
  style: PropTypes.any.isRequired,
};

FieldItem.defaultProps = {
  selectedView: undefined,
};

export const FieldsByLink = ({ mode, text, title, onChange, showFieldsBy }) => {
  const isCurrentShowFieldsBy = showFieldsBy === mode;

  return (
    // eslint-disable-next-line jsx-a11y/anchor-is-valid,jsx-a11y/click-events-have-key-events
    <a onClick={() => onChange(mode)}
       role="button"
       tabIndex={0}
       title={title}
       style={{ fontWeight: isCurrentShowFieldsBy ? 'bold' : 'normal' }}>
      {text}
    </a>
  );
};

FieldsByLink.propTypes = {
  mode: PropTypes.string.isRequired,
  showFieldsBy: PropTypes.string.isRequired,
  text: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
};


// eslint-disable-next-line react/prop-types
const FieldListRow = ({ fieldList, fields, selectedQuery, selectedView }) => ({ index, style }) => {
  return (
    <FieldItem fieldType={fieldList.get(index)}
               selectedQuery={selectedQuery}
               selectedView={selectedView}
               fields={fields}
               style={style} />
  );
};

FieldListRow.propTypes = {
  fieldList: PropTypes.object.isRequired,
  selectedQuery: PropTypes.string.isRequired,
  selectedView: PropTypes.string,
  fields: PropTypes.object.isRequired,
};

FieldListRow.defaultProps = {
  selectedView: undefined,
};

export const FieldListWrap = ({ fields, allFields, showFieldsBy, viewMetadata, filter }) => {
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
  showFieldsBy: PropTypes.string,
  viewMetadata: PropTypes.object.isRequired,
  filter: PropTypes.string,
};

FieldListWrap.defaultProps = {
  filter: undefined,
  showFieldsBy: 'all',
};

const FieldList = ({ fields, allFields, viewMetadata }) => {
  console.log('viewMetadata', viewMetadata);
  const [filter, setFilter] = useState(undefined);
  const [showFieldsBy, setShowFieldsBy] = useState('current');
  const filterFieldInputRef = useRef();

  const handleSearch = () => {
    setFilter(filterFieldInputRef.current.value);
  };

  const handleSearchReset = () => {
    setFilter(undefined);
  };

  const changeShowFieldsBy = (mode) => {
    setShowFieldsBy(mode);
  };

  return (
    <div style={{ whiteSpace: 'break-spaces' }}>
      <form className={`form-inline ${styles.filterContainer}`} onSubmit={e => e.preventDefault()}>
        <div className={`form-group has-feedback ${styles.filterInputContainer}`}>
          <input id="common-search-form-query-input"
                 className="query form-control"
                 ref={filterFieldInputRef}
                 style={{ width: '100%' }}
                 onChange={handleSearch}
                 value={filter || ''}
                 placeholder="Filter fields"
                 type="text"
                 autoComplete="off"
                 spellCheck="false" />
        </div>
        <div className="form-group">
          <Button type="reset" className="reset-button" onClick={handleSearchReset}>
            Reset
          </Button>
        </div>
      </form>

      <div style={{ marginTop: '5px', marginBottom: '0px' }}>
        List fields of{' '}
        <FieldsByLink mode="current"
                      text="current streams"
                      title="This shows fields which are (prospectively) included in the streams you have selected."
                      onChange={changeShowFieldsBy}
                      showFieldsBy={showFieldsBy} />,{' '}

        <FieldsByLink mode="all"
                      text="all"
                      title="This shows all fields, but no reserved (gl2_*) fields."
                      onChange={changeShowFieldsBy}
                      showFieldsBy={showFieldsBy} /> or{' '}

        <FieldsByLink mode="allreserved"
                      text="all including reserved"
                      title="This shows all fields, including reserved (gl2_*) fields."
                      onChange={changeShowFieldsBy}
                      showFieldsBy={showFieldsBy} />
      </div>

      <hr />

      <FieldListWrap fields={fields} allFields={allFields} showFieldsBy={showFieldsBy} viewMetadata={viewMetadata} filter={filter} />
    </div>
  );
};

FieldList.propTypes = {
  allFields: PropTypes.object.isRequired,
  fields: PropTypes.object.isRequired,
  viewMetadata: PropTypes.shape({
    id: PropTypes.number,
    activeQuery: PropTypes.string,
  }).isRequired,
};

export default connect(FieldList, { viewMetadata: ViewMetadataStore }, ({ viewMetadata: { viewMetadata } = {} }) => ({ viewMetadata }));
