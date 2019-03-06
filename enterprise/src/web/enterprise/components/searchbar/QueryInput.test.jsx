// @flow strict
import * as React from 'react';
import * as mockImmutable from 'immutable';
import { mount } from 'enzyme';

import QueryInput from './QueryInput';

jest.mock('./SearchBarAutocompletions', () => ({}));
jest.mock('enterprise/stores/FieldTypesStore', () => ({
  FieldTypesStore: {
    getInitialState: jest.fn(),
    listen: jest.fn(),
  },
}));
jest.mock('enterprise/stores/SearchParameterStore', () => ({
  SearchParameterStore: {
    getInitialState: jest.fn(() => mockImmutable.Map()),
    listen: jest.fn(),
  },
}));

class Completer {
  getCompletions = (editor, session, pos, prefix, callback) => {
    callback(null, []);
  }
}

describe('QueryInput', () => {
  it('should update its state when props change', () => {
    const wrapper = mount(<QueryInput value="*" onChange={s => Promise.resolve(s)} onExecute={() => {}} completerClass={Completer} />);
    const reactAce = wrapper.find('ReactAce');

    expect(reactAce).toHaveProp('value', '*');

    wrapper.setProps({ value: 'updated' });

    const updatedReactAce = wrapper.find('ReactAce');
    expect(updatedReactAce).toHaveProp('value', 'updated');
  });
});
