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
import type { QueryString, TimeRange } from 'views/logic/queries/Query';
import type { FiltersType } from 'views/types';

import AggregationWidgetConfig from './AggregationWidgetConfig';

import Widget from '../widgets/Widget';

export default class AggregationWidget extends Widget {
  constructor(id: string, config: AggregationWidgetConfig, filter?: string, timerange?: TimeRange, query?: QueryString, streams?: Array<string>, streamCategories?: Array<string>, filters?: FiltersType) {
    super(id, AggregationWidget.type, config, filter, timerange, query, streams, streamCategories, filters);
  }

  static type = 'AGGREGATION';

  static defaultTitle = 'Untitled Aggregation';

  static fromJSON(value) {
    const { id, config, filter, timerange, query, streams, stream_categories, filters } = value;

    return new AggregationWidget(id, AggregationWidgetConfig.fromJSON(config), filter, timerange, query, streams, stream_categories, filters);
  }

  toBuilder() {
    const { id, config, filter, timerange, query, streams, stream_categories, filters } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Map({ id, config, filter, timerange, query, streams, stream_categories, filters }));
  }

  static builder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder();
  }

  equals(other: any) {
    if (other instanceof AggregationWidget) {
      return ['id', 'config', 'filter', 'timerange', 'query', 'streams', 'stream_categories', 'filters'].every((key) => isDeepEqual(this[key], other[key]));
    }

    return false;
  }

  equalsForSearch(other: any) {
    if (other instanceof AggregationWidget) {
      return ['id', 'config', 'filter', 'timerange', 'query', 'streams', 'stream_categories', 'filters'].every((key) => isEqualForSearch(this[key], other[key]));
    }

    return false;
  }
}

class Builder extends Widget.Builder {
  build() {
    const { id, config, filter, timerange, query, streams, stream_categories, filters } = this.value.toObject();

    return new AggregationWidget(id, config, filter, timerange, query, streams, stream_categories, filters);
  }
}
