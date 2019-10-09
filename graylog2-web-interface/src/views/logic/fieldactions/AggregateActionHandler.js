// @flow strict
import { WidgetActions } from 'views/stores/WidgetStore';
import Widget from 'views/logic/widgets/Widget';
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import DataTable from 'views/components/datatable/DataTable';
import type { FieldActionHandler } from './FieldActionHandler';

const AggregateActionHandler: FieldActionHandler = ({ field, type, contexts: { widget: origWidget = Widget.empty() } }) => {
  const newWidgetBuilder = AggregationWidget.builder()
    .newId()
    .config(AggregationWidgetConfig.builder()
      .rowPivots([pivotForField(field, type)])
      .series([Series.forFunction('count()')])
      .visualization(DataTable.type)
      .build());
  if (origWidget.filter) {
    newWidgetBuilder.filter(origWidget.filter);
  }
  return WidgetActions.create(newWidgetBuilder.build());
};

export default AggregateActionHandler;
