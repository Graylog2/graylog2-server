import React from 'react';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';

import Field from 'enterprise/components/Field';
import FieldSelected from 'enterprise/components/sidebar/FieldSelected';

import QueriesActions from 'enterprise/actions/QueriesActions';
import CurrentViewStore from '../../stores/CurrentViewStore';
import SelectedFieldsActions from '../../actions/SelectedFieldsActions';

import styles from './FieldList.css'

const FieldList = createReactClass({
  propTypes: {
    fields: PropTypes.object.isRequired,
    queryId: PropTypes.string.isRequired,
  },
  mixins: [Reflux.connect(CurrentViewStore, 'currentView')],

  _renderFieldList(fields, selectedFields) {
    if (!fields) {
      return <span>No field information available.</span>;
    }
    const selectedQuery = this.state.currentView.selectedQuery;
    const selectedView = this.state.currentView.selectedView;
    const fieldList = fields.entrySeq ? fields.entrySeq()
      .sort()
      .map(([name]) => (
        <li key={`field-${name}`} className={styles.fieldListItem} >
          <FieldSelected name={name}
                         selected={selectedFields.contains(name)}
                         onToggleSelected={SelectedFieldsActions.toggle} />
          {' '}
          <Field queryId={selectedQuery}
                 viewId={selectedView}
                 name={name}
                 interactive>
            {name}
          </Field>
        </li>
      )) : null;
    return (
      <ul className={styles.fieldList}>
        {fieldList}
      </ul>
    );
  },
  render() {
    const { selectedFields } = this.props;
    return (
      <div>
        <h3>Fields</h3>
        {this._renderFieldList(this.props.fields, selectedFields)}
      </div>
    );
  },
});

export default FieldList;
