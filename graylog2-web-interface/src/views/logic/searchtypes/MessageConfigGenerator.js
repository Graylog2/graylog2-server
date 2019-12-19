// @flow strict
import MessagesWidget from '../widgets/MessagesWidget';

const MessageConfigGenerator = (widget: MessagesWidget) => {
  const { config: { decorators } } = widget;
  return [{
    type: 'messages',
    decorators,
  }];
};

export default MessageConfigGenerator;
