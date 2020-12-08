/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { Map } from 'immutable';

import isDeepEqual from 'stores/isDeepEqual';
import isEqualForSearch from 'views/stores/isEqualForSearch';

import AggregationWidgetConfig from './AggregationWidgetConfig';

import Widget from '../widgets/Widget';

export default class AggregationWidget extends Widget {
  constructor(id, config, filter, timerange, query, streams) {
    super(id, AggregationWidget.type, config, filter, timerange, query, streams);
  }

  static type = 'AGGREGATION';

  static defaultTitle = 'Untitled Aggregation'

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
      return ['id', 'config', 'filter', 'timerange', 'query', 'streams'].every((key) => isDeepEqual(this[key], other[key]));
    }

    return false;
  }

  equalsForSearch(other) {
    if (other instanceof AggregationWidget) {
      return ['id', 'config', 'filter', 'timerange', 'query', 'streams'].every((key) => isEqualForSearch(this[key], other[key]));
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
