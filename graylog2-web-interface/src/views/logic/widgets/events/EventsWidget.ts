import { Map } from 'immutable';

import Widget from 'views/logic/widgets/Widget';
import type { WidgetState } from 'views/logic/widgets/Widget';
import isDeepEqual from 'stores/isDeepEqual';
import isEqualForSearch from 'views/stores/isEqualForSearch';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';

import EventsWidgetConfig from './EventsWidgetConfig';

export default class EventsWidget extends Widget {
  constructor(id: string, config: EventsWidgetConfig) {
    super(
      id,
      EventsWidget.type,
      config,
      undefined,
      { range: 0, type: 'relative' },
      createElasticsearchQueryString('*'),
      [],
    );
  }

  static type = 'events';

  static defaultTitle = 'Untitled Events Overview';

  // eslint-disable-next-line class-methods-use-this
  get hasFixedFilters() {
    return true;
  }

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
