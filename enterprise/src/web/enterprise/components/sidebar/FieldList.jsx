import React from 'react';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import Immutable from 'immutable';

import Field from 'enterprise/components/Field';
import FieldSelected from 'enterprise/components/sidebar/FieldSelected';

import QueriesActions from 'enterprise/actions/QueriesActions';
import CurrentViewStore from '../../stores/CurrentViewStore';

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
        <li key={`field-${name}`} style={{ fontSize: '12px' }}>
          <FieldSelected name={name}
                         selected={selectedFields.contains(name)}
                         onToggleSelected={fieldName => QueriesActions.toggleField(
                           this.state.currentView.selectedView,
                           this.props.queryId,
                           fieldName,
                         )} />
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
      <ul style={{ padding: 0 }}>
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
