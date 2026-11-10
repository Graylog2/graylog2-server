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
import type { FiltersType } from 'views/types';
import type { QueryString } from 'views/logic/queries/types';
import type { TimeRange } from 'views/logic/queries/Query';

import Widget, { widgetAttributesForComparison } from './Widget';
import SwimlaneWidgetConfig from './SwimlaneWidgetConfig';
import type { WidgetState } from './Widget';

export default class SwimlaneWidget extends Widget {
  constructor(
    id: string,
    config: SwimlaneWidgetConfig,
    filter: string | undefined | null,
    timerange: TimeRange | undefined | null,
    query: QueryString | undefined | null,
    streams: Array<string>,
    streamCategories: Array<string>,
    filters?: FiltersType,
    description?: string,
    context?: string,
  ) {
    super(id, SwimlaneWidget.type, config, filter, timerange, query, streams, streamCategories, filters, description, context);
  }

  static type = 'swimlane';

  static defaultTitle = 'Untitled Swimlane';

  // eslint-disable-next-line class-methods-use-this
  get isExportable() {
    return false;
  }

  static fromJSON(value: WidgetState) {
    const { id, config, filter, timerange, query, streams, stream_categories, filters, description, context } = value;

    return new SwimlaneWidget(
      id,
      SwimlaneWidgetConfig.fromJSON(config),
      filter,
      timerange,
      query,
      streams,
      stream_categories,
      filters,
      description,
      context,
    );
  }

  equals(other: unknown) {
    if (other instanceof SwimlaneWidget) {
      return widgetAttributesForComparison.every((key) => isDeepEqual(this[key], other[key]));
    }

    return false;
  }

  get config(): SwimlaneWidgetConfig {
    return this._value.config;
  }

  toBuilder() {
    const { id, config, filter, timerange, query, streams, stream_categories, filters, description, context } =
      this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Map({ id, config, filter, timerange, query, streams, stream_categories, filters, description, context }));
  }

  static builder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder();
  }
}

class Builder extends Widget.Builder {
  build() {
    const { id, config, filter, timerange, query, streams, stream_categories, filters, description, context } =
      this.value.toObject();

    return new SwimlaneWidget(id, config, filter, timerange, query, streams, stream_categories, filters, description, context);
  }
}
