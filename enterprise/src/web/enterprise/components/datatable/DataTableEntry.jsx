import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import { flatten, get } from 'lodash';

import connect from 'stores/connect';
import CustomPropTypes from 'enterprise/components/CustomPropTypes';
import Value from 'enterprise/components/Value';
import { ViewMetadataStore } from 'enterprise/stores/ViewMetadataStore';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import * as AggregationBuilderPropTypes from '../aggregationbuilder/AggregationBuilderPropTypes';

const _c = (field, value) => ({ field, value });

class DataTableEntry extends React.Component {
  static propTypes = {
    columnPivots: AggregationBuilderPropTypes.PivotList.isRequired,
    columnPivotValues: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string)).isRequired,
    currentView: CustomPropTypes.CurrentView.isRequired,
    fields: PropTypes.instanceOf(Immutable.OrderedSet).isRequired,
    item: PropTypes.object.isRequired,
    series: AggregationBuilderPropTypes.SeriesList.isRequired,
    types: CustomPropTypes.FieldListType.isRequired,
  };

  _column = (field, value, selectedQuery, idx, type) => (
    <td key={`${selectedQuery}-${field}=${value}-${idx}`}>
      <Value field={field} type={type} value={value} queryId={selectedQuery} />
    </td>
  );

  _fieldTypeFor = (field) => {
    const fieldType = this.props.types.find(f => f.name === field);
    return fieldType ? fieldType.type : FieldType.Unknown;
  };

  render() {
    const classes = 'message-group';
    const item = this.props.item;
    const { columnPivots, fields, series, columnPivotValues } = this.props;
    const { activeQuery } = this.props.currentView;

    const fieldColumns = fields.toSeq().map(fieldName => _c(fieldName, item[fieldName])).toJS();
    const columnPivotFields = flatten(columnPivotValues.map((columnPivotValueKeys) => {
      const translatedPath = flatten(columnPivotValueKeys.map((value, idx) => [columnPivots[idx], value]));
      return series.map(({ effectiveName }) => {
        const fullPath = [].concat(translatedPath, [effectiveName]);
        const value = get(item, fullPath);
        return _c(effectiveName, value);
      });
    }));

    const columns = flatten([fieldColumns, columnPivotFields]);
    return (
      <tbody className={classes}>
        <tr className="fields-row">
          {columns.map(({ field, value }, idx) => this._column(field, value, activeQuery, idx, this._fieldTypeFor(field)))}
        </tr>
      </tbody>
    );
  }
}

export default connect(DataTableEntry, { currentView: ViewMetadataStore });
