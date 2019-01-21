// @flow strict
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import pivotForField from 'enterprise/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'enterprise/logic/aggregationbuilder/Series';
import DataTable from 'enterprise/components/datatable/DataTable';
import { ActionContext, WidgetContext } from '../ActionContext';
import FieldType from '../fieldtypes/FieldType';
import type { FieldActionHandlerWithContext } from './FieldActionHandler';

const AggregateActionHandler: FieldActionHandlerWithContext = (queryId: string, field: string, type: FieldType, context: ActionContext) => {
  let filter = null;
  if (context instanceof WidgetContext) {
    filter = context.widget.filter;
  }
  const newWidget = AggregationWidget.builder()
    .newId()
    .filter(filter)
    .config(AggregationWidgetConfig.builder()
      .rowPivots([pivotForField(field, type)])
      .series([Series.forFunction('count()')])
      .visualization(DataTable.type)
      .build())
    .build();
  return WidgetActions.create(newWidget);
};

export default AggregateActionHandler;
