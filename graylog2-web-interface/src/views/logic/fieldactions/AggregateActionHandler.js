// @flow strict
import { WidgetActions } from 'views/stores/WidgetStore';
import Widget from 'views/logic/widgets/Widget';
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import DataTable from 'views/components/datatable/DataTable';
import type { FieldActionHandler } from './FieldActionHandler';
import duplicateCommonWidgetSettings from './DuplicateCommonWidgetSettings';

const AggregateActionHandler: FieldActionHandler = ({ field, type, contexts: { widget = Widget.empty() } }) => {
  const newWidgetBuilder = AggregationWidget.builder()
    .newId()
    .config(AggregationWidgetConfig.builder()
      .rowPivots([pivotForField(field, type)])
      .series([Series.forFunction('count()')])
      .visualization(DataTable.type)
      .build());
  const newWidget = duplicateCommonWidgetSettings(newWidgetBuilder, widget).build();

  return WidgetActions.create(newWidget);
};

export default AggregateActionHandler;
