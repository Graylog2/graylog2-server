import { WidgetActions } from 'views/stores/WidgetStore';
import NumberVisualization from 'views/components/visualizations/number/NumberVisualization';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';

export default () => {
  const series = Series.forFunction('count()')
    .toBuilder()
    .config(new SeriesConfig('Message Count'))
    .build();
  WidgetActions.create(AggregationWidget.builder()
    .newId()
    .config(AggregationWidgetConfig.builder()
      .series([series])
      .visualization(NumberVisualization.type)
      .build())
    .build());
};
