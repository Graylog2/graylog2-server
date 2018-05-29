import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import { flatten, flattenDeep, get } from 'lodash';

import connect from 'stores/connect';
import Value from 'enterprise/components/Value';
import { ViewMetadataStore } from '../../stores/ViewMetadataStore';

const _c = (field, value) => ({ field, value });

class DataTableEntry extends React.Component {
  static propTypes = {
    item: PropTypes.object.isRequired,
    fields: PropTypes.instanceOf(Immutable.OrderedSet).isRequired,
  };

  _column = (field, value, selectedQuery, idx) => (
    <td key={`${selectedQuery}-${field}=${value}-${idx}`}>
      <Value field={field} value={value} queryId={selectedQuery}>{value}</Value>
    </td>
  );

  render() {
    const classes = 'message-group';
    const item = this.props.item;
    const { columnPivots, fields, series, columnPivotValues } = this.props;
    const { activeQuery } = this.props.currentView;

    const fieldColumns = fields.toSeq().map(fieldName => _c(fieldName, item[fieldName])).toJS();
    const columnPivotFields = flatten(columnPivotValues.map((columnPivotValueKeys) => {
      const translatedPath = flatten(columnPivotValueKeys.map((value, idx) => [columnPivots[idx], value]));
      return series.map((seriesName) => {
        const fullPath = [].concat(translatedPath, [seriesName]);
        const value = get(item, fullPath);
        return _c(seriesName, value);
      });
    }));

    const columns = flatten([fieldColumns, columnPivotFields]);
    return (
      <tbody className={classes}>
        <tr className="fields-row">
          {columns.map(({ field, value }, idx) => this._column(field, value, activeQuery, idx))}
        </tr>
      </tbody>
    );
  }
}

export default connect(DataTableEntry, { currentView: ViewMetadataStore });
