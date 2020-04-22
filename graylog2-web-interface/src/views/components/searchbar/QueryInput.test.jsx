// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import QueryInput from './QueryInput';
import UserPreferencesContext, { defaultUserPreferences } from '../../../contexts/UserPreferencesContext';

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
                onChange={() => {}}
                onExecute={() => {}}
                completerFactory={() => new Completer()}
                {...props} />
  );

  const _mount = (component) => {
    const wrapper = mount(component);
    return wrapper.find('ReactAce');
  };

  it('renders with minimal props', () => {
    const wrapper = mount(<SimpleQueryInput />);
    expect(wrapper).not.toBeNull();
  });

  it('triggers onChange when input is changed', () => {
    const _onChange = jest.fn();
    const aceEditor = _mount(<SimpleQueryInput onChange={_onChange} />);
    const { onChange } = aceEditor.props();

    onChange('new input');

    expect(_onChange).toHaveBeenCalledWith('new input');
  });

  it('triggers onBlur when input is blurred', () => {
    const _onBlur = jest.fn();
    const aceEditor = _mount(<SimpleQueryInput onBlur={_onBlur} />);
    const { onBlur } = aceEditor.props();

    onBlur();

    expect(_onBlur).toHaveBeenCalled();
  });

  it('disables auto completion if `enableSmartSearch` is false', () => {
    const aceEditor = _mount((
      <UserPreferencesContext.Provider value={{ ...defaultUserPreferences, enableSmartSearch: false }}>
        <SimpleQueryInput />
      </UserPreferencesContext.Provider>
    ));
    const { enableBasicAutocompletion, enableLiveAutocompletion } = aceEditor.props();

    expect(enableBasicAutocompletion).toBe(false);
    expect(enableLiveAutocompletion).toBe(false);
  });

  it('enables auto completion if `enableSmartSearch` is true', () => {
    const aceEditor = _mount((
      <UserPreferencesContext.Provider value={{ ...defaultUserPreferences, enableSmartSearch: true }}>
        <SimpleQueryInput />
      </UserPreferencesContext.Provider>
    ));
    const { enableBasicAutocompletion, enableLiveAutocompletion } = aceEditor.props();

    expect(enableBasicAutocompletion).toBe(true);
    expect(enableLiveAutocompletion).toBe(true);
  });
});
