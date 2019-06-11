import { WidgetActions } from 'enterprise/stores/WidgetStore';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'enterprise/components/datatable/DataTable';

export default function () {
  const newWidget = AggregationWidget.builder()
    .newId()
    .config(AggregationWidgetConfig.builder()
      .rowPivots([])
      .series([])
      .visualization(DataTable.type)
      .build())
    .build();
  WidgetActions.create(newWidget);
}
