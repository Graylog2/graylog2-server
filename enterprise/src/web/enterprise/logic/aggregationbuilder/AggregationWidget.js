import AggregationWidgetConfig from './AggregationWidgetConfig';
import Widget from './Widget';

export default class AggregationWidget extends Widget {
  constructor(id, config) {
    super(id, 'AGGREGATION', config);
  }

  static fromJSON(value) {
    const { id, config } = value;
    return new AggregationWidget(id, AggregationWidgetConfig.fromJSON(config));
  }
}