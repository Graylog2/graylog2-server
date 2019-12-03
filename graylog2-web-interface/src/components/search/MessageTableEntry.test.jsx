import React from 'react';
import Immutable from 'immutable';
import DateTime from 'logic/datetimes/DateTime';
import {} from 'jquery-ui/ui/version';
import {} from 'jquery-ui/ui/effect';
import {} from 'jquery-ui/ui/plugin';
import {} from 'jquery-ui/ui/widget';
import {} from 'jquery-ui/ui/widgets/mouse';
import { mount } from 'theme/enzymeWithTheme';

import MessageTableEntry from 'components/search/MessageTableEntry';

describe('<MessageTableEntry />', () => {
  DateTime.getBrowserTimezone = () => { return 'Europe/Berlin'; };
  const allStreams = Immutable.List([
    { id: '01', description: 'stream1' },
    { id: '02', description: 'stream2' },
  ]);
  const streams = Immutable.Map({ '01': { id: '01', description: 'stream1' } });
  const inputs = Immutable.Map({ '00001': { id: '00001', title: 'syslog', name: 'syslog' } });
  const message = {
    fields: { message: '2018-01-22T15:36:02.189Z foo', timestamp: '2018-01-22T15:36:02.189Z' },
    formatted_fields: { message: '2018-01-22T15:36:02.189Z foo', timestamp: '2018-01-22T15:36:02.189Z' },
    highlight_ranges: {},
    id: '01',
    index: '01',
  };
  const nodes = Immutable.Map({});

  describe('rendering', () => {
    it('should render a MessageTableEntry', () => {
      const wrapper = mount(
        <table>
          <MessageTableEntry allStreams={allStreams}
                             allStreamsLoaded
                             disableSurroundingSearch
                             expandAllRenderAsync={false}
                             expanded
                             inputs={inputs}
                             searchConfig={{}}
                             message={message}
                             nodes={nodes}
                             streams={streams}
                             toggleDetail={jest.fn} />
        </table>,
      );
      expect(wrapper).toMatchSnapshot();
    });
  });

  describe('timezone handling', () => {
    it('should render a in the UTC timezone', () => {
      const wrapper = mount(
        <table>
          <MessageTableEntry allStreams={allStreams}
                             allStreamsLoaded
                             disableSurroundingSearch
                             expandAllRenderAsync={false}
                             expanded
                             inputs={inputs}
                             searchConfig={{}}
                             message={message}
                             nodes={nodes}
                             streams={streams}
                             toggleDetail={jest.fn} />
        </table>,
      );
      expect(wrapper.find('time').at(1).text()).toEqual('2018-01-22 16:36:02.189 +01:00');
    });

    it('should render a in the USA/Honolulu timezone', () => {
      DateTime.getBrowserTimezone = () => { return 'Pacific/Honolulu'; };
      const wrapper = mount(
        <table>
          <MessageTableEntry allStreams={allStreams}
                             allStreamsLoaded
                             disableSurroundingSearch
                             expandAllRenderAsync={false}
                             expanded
                             inputs={inputs}
                             searchConfig={{}}
                             message={message}
                             nodes={nodes}
                             streams={streams}
                             toggleDetail={jest.fn} />
        </table>,
      );
      expect(wrapper.find('time').at(1).text()).toEqual('2018-01-22 05:36:02.189 -10:00');
    });
  });
});
