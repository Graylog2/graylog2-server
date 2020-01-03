import { Map, is } from 'immutable';

import Widget from './Widget';
import MessagesWidgetConfig from './MessagesWidgetConfig';

export default class MessagesWidget extends Widget {
  constructor(id, config, filter, timerange, query, streams) {
    super(id, MessagesWidget.type, config, filter, timerange, query, streams);
  }

  static type = 'messages';

  static fromJSON(value) {
    const { id, config, filter, timerange, query, streams } = value;
    return new MessagesWidget(id, MessagesWidgetConfig.fromJSON(config), filter, timerange, query, streams);
  }

  equals(other) {
    if (other instanceof MessagesWidget) {
      return ['id', 'config', 'filter', 'timerange', 'query', 'streams'].every(key => is(this[key], other[key]));
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

  static isMessagesWidget(widget = {}) {
    return widget.type === MessagesWidget.type;
  }
}

class Builder extends Widget.Builder {
  build() {
    const { id, config, filter, timerange, query, streams } = this.value.toObject();
    return new MessagesWidget(id, config, filter, timerange, query, streams);
  }
}
