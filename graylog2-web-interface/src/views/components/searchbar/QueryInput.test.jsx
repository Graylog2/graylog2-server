// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import QueryInput from './QueryInput';

jest.mock('./SearchBarAutocompletions', () => ({}));
jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesStore: {
    getInitialState: jest.fn(),
    listen: jest.fn(),
  },
}));

class Completer {
  getCompletions = (editor, session, pos, prefix, callback) => {
    callback(null, []);
  }
}

describe('QueryInput', () => {
  const SimpleQueryInput = (props) => (
    <QueryInput value="*"
                onChange={(s) => Promise.resolve(s)}
                onExecute={() => {}}
                completerClass={Completer}
                {...props} />
  );

  it('should update its state when props change', () => {
    const wrapper = mount(<SimpleQueryInput />);
    const reactAce = wrapper.find('ReactAce');

    expect(reactAce).toHaveProp('value', '*');

    wrapper.setProps({ value: 'updated' });

    const updatedReactAce = wrapper.find('ReactAce');
    expect(updatedReactAce).toHaveProp('value', 'updated');
  });
  it('does not try to close popup if it does not exist while executing query', (done) => {
    const onExecute = jest.fn();
    const wrapper = mount(<SimpleQueryInput onExecute={onExecute} />);

    wrapper.find('QueryInput').instance()._onExecute({});

    setImmediate(() => {
      expect(onExecute).toHaveBeenCalledWith('*');
      done();
    });
  });
  it('does not trigger onChange/onExecute if input receives blur without changed value', () => {
    const onBlur = jest.fn();
    const onChange = jest.fn();
    const onExecute = jest.fn();
    const currentQueryString = 'source:example.com';
    const wrapper = mount((<SimpleQueryInput onExecute={onExecute}
                                             onChange={onChange}
                                             onBlur={onBlur}
                                             value={currentQueryString} />));

    const { onBlur: _onBlur } = wrapper.find('ReactAce').props();

    _onBlur().then(() => {
      expect(onExecute).not.toHaveBeenCalled();
      expect(onChange).not.toHaveBeenCalled();
      expect(onBlur).toHaveBeenCalledWith(currentQueryString);
    });
  });
  it('does trigger onChange/onExecute if input receives blur with changed value', () => {
    const onBlur = jest.fn();
    const onChange = jest.fn((newQuery) => Promise.resolve(newQuery));
    const onExecute = jest.fn();
    const currentQueryString = 'source:example.com';
    const wrapper = mount((<SimpleQueryInput onExecute={onExecute}
                                             onChange={onChange}
                                             onBlur={onBlur}
                                             value={currentQueryString} />));

    const { onBlur: _onBlur, onChange: _onChange } = wrapper.find('ReactAce').props();

    return _onChange('source:foobar')
      .then(_onBlur)
      .then(() => {
        expect(onChange).toHaveBeenCalledWith('source:foobar');
        expect(onBlur).toHaveBeenCalledWith('source:foobar');
      });
  });
});
