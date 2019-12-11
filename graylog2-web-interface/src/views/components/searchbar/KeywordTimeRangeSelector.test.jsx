import React from 'react';
import { mount } from 'wrappedEnzyme';
import * as Immutable from 'immutable';

import { CombinedProviderMock, StoreMock } from 'helpers/mocking';

jest.mock('logic/datetimes/DateTime', () => ({ fromUTCDateTime: date => date }));

describe('KeywordTimeRangeSelector', () => {
  let KeywordTimeRangeSelector;
  const ToolsStore = {};
  beforeEach(() => {
    ToolsStore.testNaturalDate = jest.fn(() => Promise.resolve({
      from: '2018-11-14 13:52:38',
      to: '2018-11-14 13:57:38',
    }));
    jest.doMock('injection/CombinedProvider', () => new CombinedProviderMock({
      CurrentUser: { CurrentUserStore: new StoreMock('get', 'listen') },
      Tools: {
        ToolsStore,
      },
    }));
    // eslint-disable-next-line global-require
    KeywordTimeRangeSelector = require('./KeywordTimeRangeSelector');
  });

  it('renders value passed to it', () => {
    const value = Immutable.Map({ keyword: 'Last hour' });
    const wrapper = mount(<KeywordTimeRangeSelector value={value} onChange={() => {}} />);
    const input = wrapper.find('input').at(0);
    expect(input).toHaveProp('value', 'Last hour');
  });

  it('calls onChange if value changes', (done) => {
    const value = Immutable.Map({ keyword: 'Last hour' });
    const onChange = jest.fn((type, newValue) => {
      expect(type).toBe('keyword');
      expect(newValue).toBe('last year');
      done();
    });
    const wrapper = mount(<KeywordTimeRangeSelector value={value} onChange={onChange} />);
    const input = wrapper.find('input').at(0);
    input.simulate('change', { target: { value: 'last year' } });
  });

  it('calls testNaturalDate', () => {
    const value = Immutable.Map({ keyword: 'Last hour' });
    expect(ToolsStore.testNaturalDate).not.toHaveBeenCalled();

    mount(<KeywordTimeRangeSelector value={value} onChange={() => {}} />);

    expect(ToolsStore.testNaturalDate).toHaveBeenCalledWith('Last hour');
  });

  it('sets validation state to error if initial value is empty', () => {
    const value = Immutable.Map({ keyword: '' });

    const wrapper = mount(<KeywordTimeRangeSelector value={value} onChange={() => {}} />);

    expect(wrapper.find('FormGroup')).toHaveProp('validationState', 'error');
  });

  it('sets validation state to error if parsing fails initially', (done) => {
    const value = Immutable.Map({ keyword: 'invalid' });
    ToolsStore.testNaturalDate = () => Promise.reject();

    const wrapper = mount(<KeywordTimeRangeSelector value={value} onChange={() => {}} />);

    setImmediate(() => {
      wrapper.update();
      expect(wrapper.find('FormGroup')).toHaveProp('validationState', 'error');
      done();
    });
  });

  it('sets validation state to error if parsing fails after changing input', (done) => {
    const value = Immutable.Map({ keyword: 'last week' });
    ToolsStore.testNaturalDate = () => Promise.reject();

    const onChange = jest.fn();

    const wrapper = mount(<KeywordTimeRangeSelector value={value} onChange={onChange} />);

    const input = wrapper.find('input').at(0);
    input.simulate('change', { target: { value: 'invalid' } });

    setImmediate(() => {
      wrapper.update();
      expect(wrapper.find('FormGroup')).toHaveProp('validationState', 'error');
      expect(onChange).not.toHaveBeenCalled();
      done();
    });
  });

  it('resets validation state if parsing succeeds after changing input', (done) => {
    const value = Immutable.Map({ keyword: 'last week' });

    const onChange = jest.fn();

    const wrapper = mount(<KeywordTimeRangeSelector value={value} onChange={onChange} />);

    const input = wrapper.find('input').at(0);
    input.simulate('change', { target: { value: 'last hour' } });

    setImmediate(() => {
      wrapper.update();
      expect(wrapper.find('FormGroup')).toHaveProp('validationState', null);
      expect(onChange).toHaveBeenLastCalledWith('keyword', 'last hour');
      done();
    });
  });

  it('shows keyword preview if parsing succeeded', (done) => {
    const value = Immutable.Map({ keyword: 'last five minutes' });

    const wrapper = mount(<KeywordTimeRangeSelector value={value} onChange={() => {}} />);

    setImmediate(() => {
      wrapper.update();
      expect(wrapper.find('Alert').at(0)).toIncludeText('Preview:2018-11-14 13:52:38 to 2018-11-14 13:57:38');
      done();
    });
  });

  it('does not show keyword preview if parsing fails', (done) => {
    ToolsStore.testNaturalDate = () => Promise.reject();
    const value = Immutable.Map({ keyword: 'invalid' });

    const wrapper = mount(<KeywordTimeRangeSelector value={value} onChange={() => {}} />);

    setImmediate(() => {
      wrapper.update();
      expect(wrapper.find('Alert')).not.toExist();
      done();
    });
  });

  it('shows keyword preview if parsing succeeded after changing input', (done) => {
    const success = ToolsStore.testNaturalDate;
    const value = Immutable.Map({ keyword: '' });

    const wrapper = mount(<KeywordTimeRangeSelector value={value} onChange={() => {}} />);

    ToolsStore.testNaturalDate = success;
    const input = wrapper.find('input').at(0);
    input.simulate('change', { target: { value: 'last hour' } });

    setImmediate(() => {
      wrapper.update();
      expect(wrapper.find('Alert').at(0)).toIncludeText('Preview:2018-11-14 13:52:38 to 2018-11-14 13:57:38');
      done();
    });
  });

  it('does not show keyword preview if parsing fails after changing input', (done) => {
    const value = Immutable.Map({ keyword: 'last week' });

    const wrapper = mount(<KeywordTimeRangeSelector value={value} onChange={() => {}} />);

    ToolsStore.testNaturalDate = () => Promise.reject();
    const input = wrapper.find('input').at(0);
    input.simulate('change', { target: { value: 'invalid' } });

    setImmediate(() => {
      wrapper.update();
      expect(wrapper.find('Alert')).not.toExist();
      done();
    });
  });
});
