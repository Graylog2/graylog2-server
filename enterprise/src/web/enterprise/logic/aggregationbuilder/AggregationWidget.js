import { Map } from 'immutable';

import AggregationWidgetConfig from './AggregationWidgetConfig';
import Widget from '../widgets/Widget';

export default class AggregationWidget extends Widget {
  constructor(id, config, filter) {
    super(id, AggregationWidget.type, config, filter);
  }

  static type = 'AGGREGATION';

  static fromJSON(value) {
    const { id, config, filter } = value;
    return new AggregationWidget(id, AggregationWidgetConfig.fromJSON(config), filter);
  }

  toBuilder() {
    const { id, config, filter } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Map({ id, config, filter }));
  }

  static builder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }
}

class Builder extends Widget.Builder {
  build() {
    const { id, config, filter } = this.value.toObject();
    return new AggregationWidget(id, config, filter);
  }
}