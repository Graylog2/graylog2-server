// @flow strict
import MessageConfigGenerator from './MessageConfigGenerator';
import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';

describe('MessageConfigGenerator', () => {
  it('generates basic search type from message widget', () => {
    const widget = MessagesWidget.builder().config(
      MessagesWidgetConfig.builder()
        .decorators([])
        .build(),
    ).build();

    const result = MessageConfigGenerator(widget);

    expect(result).toEqual([{ decorators: [], type: 'messages' }]);
  });
  it('adds decorators to search type', () => {
    const decorators = [
      { id: 'decorator1', type: 'something' },
      { id: 'decorator2', type: 'something else' },
    ];
    const widget = MessagesWidget.builder().config(
      MessagesWidgetConfig.builder()
        .decorators(decorators)
        .build(),
    ).build();

    const result = MessageConfigGenerator(widget);

    expect(result).toEqual([{
      decorators: [
        { id: 'decorator1', type: 'something' },
        { id: 'decorator2', type: 'something else' },
      ],
      type: 'messages',
    }]);
  });
});
