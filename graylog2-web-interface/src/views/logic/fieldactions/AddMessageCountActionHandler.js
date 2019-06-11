import { WidgetActions } from 'enterprise/stores/WidgetStore';
import NumberVisualization from 'enterprise/components/visualizations/number/NumberVisualization';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'enterprise/logic/aggregationbuilder/Series';
import SeriesConfig from 'enterprise/logic/aggregationbuilder/SeriesConfig';

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
