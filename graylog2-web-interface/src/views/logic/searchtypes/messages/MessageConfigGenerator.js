// @flow strict
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';

const MessageConfigGenerator = (widget: MessagesWidget) => {
  const { config: { decorators, sort: widgetSort } } = widget;

  return [{
    type: 'messages',
    decorators,
    sort: widgetSort.map((sort) => new MessageSortConfig(sort.field, sort.direction)),
  }];
};

export default MessageConfigGenerator;
