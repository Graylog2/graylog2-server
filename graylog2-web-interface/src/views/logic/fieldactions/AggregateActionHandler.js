// @flow strict
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import Widget from 'enterprise/logic/widgets/Widget';
import pivotForField from 'enterprise/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'enterprise/logic/aggregationbuilder/Series';
import DataTable from 'enterprise/components/datatable/DataTable';
import type { ActionContexts } from '../ActionContext';
import FieldType from '../fieldtypes/FieldType';
import type { FieldActionHandler } from './FieldActionHandler';

const AggregateActionHandler: FieldActionHandler = (queryId: string, field: string, type: FieldType, context: ActionContexts) => {
  const { widget: origWidget = Widget.empty() } = context;
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
