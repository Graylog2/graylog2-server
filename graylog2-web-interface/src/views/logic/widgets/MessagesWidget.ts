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
import type { FiltersType } from 'views/types';

import Widget from './Widget';
import MessagesWidgetConfig from './MessagesWidgetConfig';
import type { WidgetState } from './Widget';

import type { QueryString, TimeRange } from '../queries/Query';

export default class MessagesWidget extends Widget {
  // eslint-disable-next-line default-param-last
  constructor(id: string, config: any, filter: string | undefined | null, timerange: TimeRange | undefined | null, query: QueryString | undefined | null, streams: Array<string> = [], filters?: FiltersType) {
    super(id, MessagesWidget.type, config, filter, timerange, query, streams, filters);
  }

  static type = 'messages';

  static defaultTitle = 'Untitled Message Table';

  // eslint-disable-next-line class-methods-use-this
  get isExportable() {
    return true;
  }

  static fromJSON(value: WidgetState) {
    const { id, config, filter, timerange, query, streams, filters } = value;

    return new MessagesWidget(id, MessagesWidgetConfig.fromJSON(config), filter, timerange, query, streams, filters);
  }

  equals(other: any) {
    if (other instanceof MessagesWidget) {
      return ['id', 'config', 'filter', 'timerange', 'query', 'streams', 'filters'].every((key) => isDeepEqual(this._value[key], other[key]));
    }

    return false;
  }

  get config(): MessagesWidgetConfig {
    return this._value.config;
  }

  equalsForSearch(other: any) {
    if (other instanceof MessagesWidget) {
      return ['id', 'config', 'filter', 'timerange', 'query', 'streams', 'filters'].every((key) => isEqualForSearch(this._value[key], other[key]));
    }

    return false;
  }

  toBuilder() {
    const { id, config, filter, timerange, query, streams, filters } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Map({ id, config, filter, timerange, query, streams, filters }));
  }

  static builder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder();
  }

  static isMessagesWidget(widget: Widget) {
    return widget && widget.type === MessagesWidget.type;
  }
}

class Builder extends Widget.Builder {
  build(): MessagesWidget {
    const { id, config, filter, timerange, query, streams, filters } = this.value.toObject();

    return new MessagesWidget(id, config, filter, timerange, query, streams, filters);
  }
}
