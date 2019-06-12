import { WidgetActions } from 'views/stores/WidgetStore';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable/DataTable';

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
