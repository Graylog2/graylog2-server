import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import { get } from 'lodash';

import connect from 'stores/connect';
import Value from 'enterprise/components/Value';
import CurrentViewStore from 'enterprise/stores/CurrentViewStore';

const _c = (field, value) => ({ field, value });

class DataTableEntry extends React.Component {
  static propTypes = {
    item: PropTypes.object.isRequired,
    fields: PropTypes.instanceOf(Immutable.OrderedSet).isRequired,
  };

  _column = (field, value, selectedQuery) => (
    <td key={field}>
      <Value field={field} value={value} queryId={selectedQuery}>{value}</Value>
    </td>
  );

  render() {
    const classes = 'message-group';
    const item = this.props.item;
    const { columnPivots, fields, series, columnPivotValues } = this.props;
    const { selectedQuery } = this.props.currentView;
    const columns = [];
    fields.toSeq().forEach(fieldName => columns.push(_c(fieldName, item[fieldName])));
    columnPivots.forEach((fieldName) => {
      const values = columnPivotValues.find(f => f.field === fieldName).values || [];
      values.forEach(value => series.forEach(s => columns.push(_c(value, get(item, `${fieldName}.${value}.${s}`)))));
    });
    return (
      <tbody className={classes}>
        <tr className="fields-row">
          {columns.map(({ field, value }) => this._column(field, value, selectedQuery))}
        </tr>
      </tbody>
    );
  }
}

export default connect(DataTableEntry, { currentView: CurrentViewStore });
