// @flow strict
import MessagesWidget from 'views/logic/widgets/MessagesWidget';

const MessageConfigGenerator = (widget: MessagesWidget) => {
  const { config: { decorators, sort } } = widget;
  return [{
    type: 'messages',
    decorators,
    sort: sort.map(({ direction, field }) => ({ order: direction.direction === 'Descending' ? 'DESC' : 'ASC', field })),
  }];
};

export default MessageConfigGenerator;
