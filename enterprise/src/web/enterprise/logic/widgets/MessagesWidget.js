import { Map } from 'immutable';

import Widget from './Widget';
import MessagesWidgetConfig from './MessagesWidgetConfig';

export default class MessagesWidget extends Widget {
  constructor(id, config, filter) {
    super(id, MessagesWidget.type, config, filter);
  }

  static type = 'messages';

  static fromJSON(value) {
    const { id, config, filter } = value;
    return new MessagesWidget(id, MessagesWidgetConfig.fromJSON(config), filter);
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
    return new MessagesWidget(id, config, filter);
  }
}