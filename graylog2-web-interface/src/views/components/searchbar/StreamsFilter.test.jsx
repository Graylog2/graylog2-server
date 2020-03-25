// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import StreamsFilter from './StreamsFilter';

describe('StreamsFilter', () => {
  it('sorts stream names', () => {
    const streams = [
      { key: 'One Stream', value: 'streamId1' },
      { key: 'another Stream', value: 'streamId2' },
      { key: 'Yet another Stream', value: 'streamId3' },
      { key: '101 Stream', value: 'streamId4' },
    ];
    const wrapper = mount(<StreamsFilter streams={streams} onChange={() => {}} />);
    const { options } = wrapper.find('Select').first().props();

    expect(options).toEqual([
      { key: '101 Stream', value: 'streamId4' },
      { key: 'another Stream', value: 'streamId2' },
      { key: 'One Stream', value: 'streamId1' },
      { key: 'Yet another Stream', value: 'streamId3' },
    ]);
  });
});
