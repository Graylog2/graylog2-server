// @flow strict
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import MessageConfigGenerator from './MessageConfigGenerator';

describe('MessageConfigGenerator', () => {
  it('generates basic search type from message widget', () => {
    // $FlowFixMe: We need to force this being a `MessagesWidget`
    const widget: MessagesWidget = MessagesWidget.builder()
      .config(
        MessagesWidgetConfig.builder()
          .decorators([])
          .build(),
      ).build();

    const result = MessageConfigGenerator(widget);

    expect(result).toEqual([{ decorators: [], type: 'messages' }]);
  });
  it('adds decorators to search type', () => {
    const decorators = [
      { id: 'decorator1', type: 'something', config: {}, stream: null, order: 0 },
      { id: 'decorator2', type: 'something else', config: {}, stream: null, order: 1 },
    ];
    // $FlowFixMe: We need to force this being a `MessagesWidget`
    const widget: MessagesWidget = MessagesWidget.builder()
      .config(
        MessagesWidgetConfig.builder()
          .decorators(decorators)
          .build(),
      ).build();

    const result = MessageConfigGenerator(widget);

    expect(result).toEqual([{
      decorators: [
        { id: 'decorator1', type: 'something', config: {}, stream: null, order: 0 },
        { id: 'decorator2', type: 'something else', config: {}, stream: null, order: 1 },
      ],
      type: 'messages',
    }]);
  });
});
