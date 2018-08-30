import React from 'react';
import { mount } from 'enzyme';
import { CombinedProviderMock, StoreMock, StoreProviderMock } from 'helpers/mocking';

describe('AlertStatus', () => {

  const SearchStore = StoreMock('searchSurroundingMessages');
  const combinedProviderMock = new CombinedProviderMock({
    Search: { SearchStore },
  });

  const CurrentUserStore = StoreMock('listen', 'get');
  const StreamsStore = StoreMock('listen', ['listStreams', () => { return { then: jest.fn() }; }], 'availableStreams');
  const storeProviderMock = new StoreProviderMock({
    Streams: StreamsStore,
    CurrentUser: CurrentUserStore,
  });

  const SearchConfigStore = StoreMock('listSearchesClusterConfig', 'configurations', 'listen');

  jest.doMock('injection/StoreProvider', () => storeProviderMock);
  jest.doMock('injection/CombinedProvider', () => combinedProviderMock);
  jest.doMock('enterprise/stores/StreamsStore', () => ({ StreamsStore: StreamsStore }));
  jest.doMock('legacy/result-histogram', () => 'Histogram');
  jest.doMock('components/search', () => ({ MessageTablePaginator: 'messageTablePaginator' }));
  jest.doMock('enterprise/stores/SearchConfigStore', () => ({ SearchConfigStore: SearchConfigStore }));

  const AlertStatus = require('./AlertStatus').default;

  it('should render a triggered AlertStatus', () => {
    const config = {
      title: 'Title',
      triggered: true,
      bgColor: '#333',
      triggeredBgColor: '#000',
      text: 'Triggered',
    };
    const wrapper = mount(<AlertStatus config={config} onChange={() => {}} onFinishEditing={() => {}} />);
    const header = wrapper.find('h1');
    expect(header.text()).toBe(config.text);
    const divStyle = header.parent().get(0).props.style;
    expect(divStyle).toHaveProperty('backgroundColor', config.triggeredBgColor);
  });

  it('should render a not triggered AlertStatus', () => {
    const config = {
      title: 'Title',
      triggered: false,
      bgColor: '#333',
      triggeredBgColor: '#000',
      text: 'Not triggered',
    };
    const wrapper = mount(<AlertStatus config={config} onChange={() => {}} onFinishEditing={() => {}} />);
    const header = wrapper.find('h1');
    expect(header.text()).toBe(config.text);
    const divStyle = header.parent().get(0).props.style;
    expect(divStyle).toHaveProperty('backgroundColor', config.bgColor);
  });
});
