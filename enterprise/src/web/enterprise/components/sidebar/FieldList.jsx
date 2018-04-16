import React from 'react';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';

import EventHandlersThrottler from 'util/EventHandlersThrottler';
import Field from 'enterprise/components/Field';
import FieldSelected from 'enterprise/components/sidebar/FieldSelected';

import CurrentViewStore from 'enterprise/stores/CurrentViewStore';
import CurrentSelectedFieldsActions from 'enterprise/actions/CurrentSelectedFieldsActions';
import CurrentSelectedFieldsStore from 'enterprise/stores/CurrentSelectedFieldsStore';

import styles from './FieldList.css';
import FieldTypeIcon from './FieldTypeIcon';

const FieldList = createReactClass({
  propTypes: {
    allFields: PropTypes.object.isRequired,
    fields: PropTypes.object.isRequired,
    selectedFields: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(CurrentViewStore, 'currentView')],

  componentDidMount() {
    this._updateHeight();
    window.addEventListener('scroll', this._onScroll);
  },

  componentDidUpdate(prevProps) {
    if (this.props.maximumHeight !== prevProps.maximumHeight) {
      this._updateHeight();
    }
  },

  componentWillUnmount() {
    window.removeEventListener('scroll', this._onScroll);
  },

  eventsThrottler: new EventHandlersThrottler(),
  MINIMUM_FIELDS_HEIGHT: 50,

  _onScroll() {
    this.eventsThrottler.throttle(this._updateHeight, 30);
  },

  _updateHeight() {
    const fieldsContainer = this.fieldList;

    const maxHeight = this.props.maximumHeight -
      fieldsContainer.getBoundingClientRect().top;

    this.setState({ maxFieldsHeight: Math.max(maxHeight, this.MINIMUM_FIELDS_HEIGHT) });
  },

  _renderField({ fields, fieldType, selectedQuery, selectedView, selectedFields }) {
    const name = fieldType.get('field_name');
    const type = fieldType.get('physical_type');
    const disabled = !fields.find(f => f.get('field_name') === name);

    return (
      <li key={`field-${name}`} className={styles.fieldListItem} >
        <FieldSelected name={name}
                       selected={selectedFields.contains(name)}
                       onToggleSelected={CurrentSelectedFieldsActions.toggle} />
        {' '}
        <FieldTypeIcon type={type} />
        {' '}
        <Field queryId={selectedQuery}
               viewId={selectedView}
               disabled={disabled}
               menuContainer={document && document.getElementById('sidebar')}
               name={name}
               type={type}
               interactive>
          {name}
        </Field>
      </li>
    );
  },
  _renderFieldList({ fields, selectedFields, allFields }) {
    if (!fields) {
      return <span>No field information available.</span>;
    }
    const selectedQuery = this.state.currentView.selectedQuery;
    const selectedView = this.state.currentView.selectedView;
    const fieldList = allFields
      .sort()
      .map(fieldType => this._renderField({ fieldType, selectedQuery, selectedView, selectedFields, fields }));
    return (
      <ul ref={(elem) => { this.fieldList = elem; }}
          style={{ maxHeight: this.state.maxFieldsHeight }}
          className={styles.fieldList}>
        {fieldList}
      </ul>
    );
  },
  render() {
    const { allFields, fields, selectedFields } = this.props;
    return (
      <div>
        {this._renderFieldList({ fields, selectedFields, allFields })}
      </div>
    );
  },
});

export default FieldList;
