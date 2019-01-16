import React from 'react';
import { mount } from 'enzyme';

import QueryInput from './QueryInput';
jest.mock('./SearchBarAutocompletions', () => ({}));

class Completer {
  getCompletions = (editor, session, pos, prefix, callback) => {
    callback(null, []);
  }
}

describe('QueryInput', () => {
  it('should update its state when props change', () => {
    const wrapper = mount(<QueryInput value="*" onChange={() => {}} onExecute={() => {}} completerClass={Completer} />);
    const reactAce = wrapper.find('ReactAce');

    expect(reactAce).toHaveProp('value', '*');

    wrapper.setProps({ value: 'updated' });

    const updatedReactAce = wrapper.find('ReactAce');
    expect(updatedReactAce).toHaveProp('value', 'updated');
  });
});
