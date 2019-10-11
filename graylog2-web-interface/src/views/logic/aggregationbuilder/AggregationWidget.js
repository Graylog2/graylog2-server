import { Map, is } from 'immutable';

import AggregationWidgetConfig from './AggregationWidgetConfig';
import Widget from '../widgets/Widget';

export default class AggregationWidget extends Widget {
  constructor(id, config, filter, timerange, query, streams) {
    super(id, AggregationWidget.type, config, filter, timerange, query, streams);
  }

  static type = 'AGGREGATION';

  static fromJSON(value) {
    const { id, config, filter, timerange, query, streams } = value;
    return new AggregationWidget(id, AggregationWidgetConfig.fromJSON(config), filter, timerange, query, streams);
  }

  toBuilder() {
    const { id, config, filter, timerange, query, streams } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Map({ id, config, filter, timerange, query, streams }));
  }

  static builder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }

  equals(other) {
    if (other instanceof AggregationWidget) {
      return ['id', 'config', 'filter', 'timerange', 'query', 'streams'].every(key => is(this[key], other[key]));
    }
    return false;
  }
}

class Builder extends Widget.Builder {
  build() {
    const { id, config, filter, timerange, query, streams } = this.value.toObject();
    return new AggregationWidget(id, config, filter, timerange, query, streams);
  }
}
