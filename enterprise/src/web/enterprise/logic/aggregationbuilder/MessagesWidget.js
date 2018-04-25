import Widget from './Widget';
import MessagesWidgetConfig from './MessagesWidgetConfig';

export default class MessagesWidget extends Widget {
  constructor(id, config) {
    super(id, 'messages', config);
  }

  static fromJSON(value) {
    const { id, config } = value;
    return new MessagesWidget(id, MessagesWidgetConfig.fromJSON(config));
  }
}