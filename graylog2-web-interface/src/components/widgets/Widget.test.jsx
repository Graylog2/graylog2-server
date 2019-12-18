import React from 'react';
import { mount } from 'wrappedEnzyme';
import Widget from 'components/widgets/Widget';

describe('<Widget />', () => {
  const widget = {
    id: 'widget-1',
    type: 'foo',
    description: 'A generic widget',
    config: {
      stream_id: 'stream-1',
      timerange: {
        type: 'UTC',
      },
    },
  };
  describe('locked and unlocked', () => {
    it('should render a locked Widget', () => {
      const wrapper = mount(<Widget id={widget.id}
                                    key={`widget-${widget.id}`}
                                    widget={widget}
                                    dashboardId="dashboard-id"
                                    locked
                                    shouldUpdate
                                    streamIds={{ 'stream-1': 'stream-1' }} />);
      expect(wrapper.find('.widget-replay').find('Button').at(0).prop('title')).toEqual('Replay search');
      expect(wrapper.find('.widget-edit').exists()).toBeFalsy();
    });

    it('should render a unlocked Widget', () => {
      const wrapper = mount(<Widget id={widget.id}
                                    key={`widget-${widget.id}`}
                                    widget={widget}
                                    dashboardId="dashboard-id"
                                    locked={false}
                                    shouldUpdate
                                    streamIds={{ 'stream-1': 'stream-1' }} />);
      expect(wrapper.find('.widget-edit').find('Button').at(0).prop('title')).toEqual('Edit widget');
      expect(wrapper.find('.widget-replay').exists()).toBeFalsy();
    });
  });

  describe('disable and enable replay', () => {
    it('should not render a replay button if the user has no streams to read', () => {
      const wrapper = mount(<Widget id={widget.id}
                                    key={`widget-${widget.id}`}
                                    widget={widget}
                                    dashboardId="dashboard-id"
                                    locked
                                    shouldUpdate
                                    streamIds={{}} />);
      expect(wrapper.find('.widget-replay').exists()).toBeFalsy();
    });
  });
});
