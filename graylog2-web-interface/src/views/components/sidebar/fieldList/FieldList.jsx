import React from 'react';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { is } from 'immutable';
import { isEqual } from 'lodash';
import { FixedSizeList as List } from 'react-window';

import { Button } from 'components/graylog';
import Field from 'views/components/Field';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';
import FieldTypeIcon from './FieldTypeIcon';

import styles from './FieldList.css';

const isReservedField = (fieldName) => MessageFieldsFilter.FILTERED_FIELDS.includes(fieldName);

const FieldList = createReactClass({
  propTypes: {
    allFields: PropTypes.object.isRequired,
    listHeight: PropTypes.number,
    fields: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(ViewMetadataStore, 'viewMetadata')],

  getDefaultProps() {
    return {
      listHeight: 50,
    };
  },

  getInitialState() {
    return {
      filter: undefined,
      showFieldsBy: 'current',
    };
  },

  shouldComponentUpdate(nextProps, nextState) {
    const { allFields, fields, listHeight } = this.props;
    if (!isEqual(nextProps.listHeight, listHeight)) {
      return true;
    }

    if (!isEqual(this.state, nextState)) {
      return true;
    }
    return !is(nextProps.allFields, allFields) || !is(nextProps.fields, fields);
  },

  _renderField({ fields, fieldType, selectedQuery, selectedView, style }) {
    const { name, type } = fieldType;
    const disabled = !fields.find((f) => f.name === name);

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
  },

  _fieldsToShow(fields, allFields, showFieldsBy = 'all') {
    const isNotReservedField = (f) => !isReservedField(f.name);
    switch (showFieldsBy) {
      case 'all':
        return allFields.filter(isNotReservedField);
      case 'allreserved':
        return allFields;
      case 'current':
      default:
        return fields.filter(isNotReservedField);
    }
  },

  _renderFieldList({ fields, allFields, showFieldsBy }) {
    const {
      filter,
      viewMetadata: {
        id: selectedView,
        activeQuery: selectedQuery,
      },
    } = this.state;
    const { listHeight } = this.props;

    if (!fields) {
      return <span>No field information available.</span>;
    }
    const fieldFilter = filter ? ((field) => field.name.toLocaleUpperCase().includes(filter.toLocaleUpperCase())) : () => true;
    const fieldsToShow = this._fieldsToShow(fields, allFields, showFieldsBy);
    const fieldList = fieldsToShow
      .filter(fieldFilter)
      .sortBy((field) => field.name.toLocaleUpperCase());

    if (fieldList.isEmpty()) {
      return <i>No fields to show. Try changing your filter term or select a different field set above.</i>;
    }
    const Row = ({ index, style }) => this._renderField({ fieldType: fieldList.get(index), selectedQuery, selectedView, fields, style });

    return (
      <div ref={(elem) => { this.fieldList = elem; }}>
        <List height={listHeight}
              itemCount={fieldList.size}
              itemSize={17}>
          {Row}
        </List>
      </div>
    );
  },
  handleSearch(e) {
    const filter = e.target.value;
    this.setState({ filter });
  },
  handleSearchReset() {
    this.setState({ filter: undefined });
  },
  changeShowFieldsBy(mode) {
    this.setState({ showFieldsBy: mode });
  },
  isCurrentShowFieldsBy(mode) {
    const { showFieldsBy } = this.state;

    return showFieldsBy === mode;
  },
  showFieldsByLink(mode, text, title) {
    return (
      // eslint-disable-next-line jsx-a11y/anchor-is-valid,jsx-a11y/click-events-have-key-events
      <a onClick={() => this.changeShowFieldsBy(mode)}
         role="button"
         tabIndex={0}
         title={title}
         style={{ fontWeight: this.isCurrentShowFieldsBy(mode) ? 'bold' : 'normal' }}>
        {text}
      </a>
    );
  },
  render() {
    const { allFields, fields } = this.props;
    const { filter, showFieldsBy } = this.state;

    return (
      <div style={{ whiteSpace: 'break-spaces' }}>
        <form className={`form-inline ${styles.filterContainer}`} onSubmit={(e) => e.preventDefault()}>
          <div className={`form-group has-feedback ${styles.filterInputContainer}`}>
            <input id="common-search-form-query-input"
                   className="query form-control"
                   style={{ width: '100%' }}
                   onChange={this.handleSearch}
                   value={filter || ''}
                   placeholder="Filter fields"
                   type="text"
                   autoComplete="off"
                   spellCheck="false" />
          </div>
          <div className="form-group">
            <Button type="reset" className="reset-button" onClick={this.handleSearchReset}>
              Reset
            </Button>
          </div>
        </form>
        <div style={{ marginTop: '5px', marginBottom: '0px' }}>
          List fields of{' '}
          {this.showFieldsByLink('current', 'current streams', 'This shows fields which are (prospectively) included in the streams you have selected.')},{' '}
          {this.showFieldsByLink('all', 'all', 'This shows all fields, but no reserved (gl2_*) fields.')} or{' '}
          {this.showFieldsByLink('allreserved', 'all including reserved', 'This shows all fields, including reserved (gl2_*) fields.')} fields.
        </div>
        <hr />
        {this._renderFieldList({ fields, allFields, showFieldsBy })}
      </div>
    );
  },
});

export default FieldList;
