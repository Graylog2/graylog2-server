// @flow strict
import { Map } from 'immutable';

import isDeepEqual from 'stores/isDeepEqual';
import isEqualForSearch from 'views/stores/isEqualForSearch';
import Widget from './Widget';
import MessagesWidgetConfig from './MessagesWidgetConfig';
import type { QueryString, TimeRange } from '../queries/Query';
import type { WidgetState } from './Widget';

export default class MessagesWidget extends Widget {
  constructor(id: string, config: any, filter: ?string, timerange: ?TimeRange, query: ?QueryString, streams: Array<string> = []) {
    super(id, MessagesWidget.type, config, filter, timerange, query, streams);
  }

  static type = 'messages';

  static defaultTitle = 'Untitled Message Table';

  static fromJSON(value: WidgetState) {
    const { id, config, filter, timerange, query, streams } = value;
    return new MessagesWidget(id, MessagesWidgetConfig.fromJSON(config), filter, timerange, query, streams);
  }

  equals(other: any) {
    if (other instanceof MessagesWidget) {
      return ['id', 'config', 'filter', 'timerange', 'query', 'streams'].every((key) => isDeepEqual(this._value[key], other[key]));
    }
    return false;
  }

  equalsForSearch(other: any) {
    if (other instanceof MessagesWidget) {
      return ['id', 'config', 'filter', 'timerange', 'query', 'streams'].every((key) => isEqualForSearch(this._value[key], other[key]));
    }
    return false;
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

  static isMessagesWidget(widget: Widget) {
    return widget && widget.type === MessagesWidget.type;
  }
}

class Builder extends Widget.Builder {
  build(): MessagesWidget {
    const { id, config, filter, timerange, query, streams } = this.value.toObject();
    return new MessagesWidget(id, config, filter, timerange, query, streams);
  }
}
