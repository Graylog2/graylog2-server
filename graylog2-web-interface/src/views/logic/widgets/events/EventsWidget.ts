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

import Widget from 'views/logic/widgets/Widget';
import type { WidgetState } from 'views/logic/widgets/Widget';
import isDeepEqual from 'stores/isDeepEqual';
import isEqualForSearch from 'views/stores/isEqualForSearch';

import EventsWidgetConfig from './EventsWidgetConfig';

export default class EventsWidget extends Widget {
  constructor(id: string, config: EventsWidgetConfig) {
    super(
      id,
      EventsWidget.type,
      config,
      undefined,
      undefined,
      undefined,
      [],
    );
  }

  static type = 'events';

  static defaultTitle = 'Untitled Events Overview';

  static fromJSON(value: WidgetState) {
    const { id, config } = value;

    return new EventsWidget(id, EventsWidgetConfig.fromJSON(config));
  }

  equals(other: any) {
    if (other instanceof EventsWidget) {
      return ['id', 'config'].every((key) => isDeepEqual(this._value[key], other[key]));
    }

    return false;
  }

  equalsForSearch(other: any) {
    if (other instanceof EventsWidget) {
      return ['id', 'config'].every((key) => isEqualForSearch(this._value[key], other[key]));
    }

    return false;
  }

  toBuilder() {
    const { id, config } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Map({ id, config }));
  }

  static builder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder();
  }
}

class Builder extends Widget.Builder {
  build(): EventsWidget {
    const { id, config } = this.value.toObject();

    return new EventsWidget(id, config);
  }
}
