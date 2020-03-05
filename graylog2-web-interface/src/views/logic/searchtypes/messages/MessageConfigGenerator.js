// @flow strict
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import MessageSortConifg from 'views/logic/searchtypes/messages/MessageSortConifg';

const MessageConfigGenerator = (widget: MessagesWidget) => {
  const { config: { decorators, sort: widgetSort } } = widget;

  return [{
    type: 'messages',
    decorators,
    sort: widgetSort.map(sort => new MessageSortConifg(sort.type, sort.field, sort.direction)),
  }];
};

export default MessageConfigGenerator;
