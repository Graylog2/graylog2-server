// @flow strict
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import pivotForField from 'enterprise/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'enterprise/logic/aggregationbuilder/Series';
import DataTable from 'enterprise/components/datatable/DataTable';
import { ActionContext, WidgetContext } from '../ActionContext';

export default function (queryId: string, field: string, context: ActionContext) {
  let filter = null;
  if (context instanceof WidgetContext) {
    filter = context.widget.filter;
  }
  const newWidget = AggregationWidget.builder()
    .newId()
    .filter(filter)
    .config(AggregationWidgetConfig.builder()
      .rowPivots([pivotForField(field)])
      .series([Series.forFunction('count()')])
      .visualization(DataTable.type)
      .build())
    .build();
  WidgetActions.create(newWidget);
}
