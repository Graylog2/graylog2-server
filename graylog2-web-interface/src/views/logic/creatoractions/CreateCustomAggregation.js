// @flow strict
import View from 'views/logic/views/View';
import { WidgetActions } from 'views/stores/WidgetStore';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable/DataTable';
import type { CreatorProps } from 'views/components/sidebar/AddWidgetButton';
import { DEFAULT_TIMERANGE } from 'views/Constants';

export default function ({ view }: CreatorProps) {
  const newWidget = AggregationWidget.builder()
    .newId()
    .timerange(view.type === View.Type.Dashboard ? DEFAULT_TIMERANGE : undefined)
    .config(AggregationWidgetConfig.builder()
      .rowPivots([])
      .series([])
      .visualization(DataTable.type)
      .build())
    .build();

  return WidgetActions.create(newWidget);
}
