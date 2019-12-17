// @flow strict
import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';
import UrlWhiteListForm from './UrlWhiteListForm';

describe('UrlWhitelistForm', () => {
  let wrapper;
  const setState = jest.fn();
  const useStateSpy = jest.spyOn(React, 'useState');
  useStateSpy.mockImplementation(init => [init, setState]);
  const onUpdate = jest.fn();
  const config = {
    entries: [
      {
        id: 'f7033f1f-d50f-4323-96df-294ede41d951',
        value: 'http://localhost:8080/system/',
        title: 'testam',
        type: 'regex',
      },
      {
        id: '636a2d40-c4c5-40b9-ab3a-48cf7978e9af',
        value: 'http://localhost:8080/system/',
        title: 'test',
        type: 'regex',
      },
      {
        id: 'f28fd891-5f2d-4128-9a94-e97c1ab07a1f',
        value: 'http://localhost:8080/system/',
        title: 'test',
        type: 'literal',
      },
    ],
    disabled: false,
  };


  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('render the UrlWhitelistForm component', () => {
    it('should create new instance', () => {
      wrapper = renderer.create(<UrlWhiteListForm urls={config.entries}
                                                  disabled={config.disabled}
                                                  update={onUpdate} />);
      expect(wrapper.toJSON()).toMatchSnapshot();
    });

    it('should display Url form list table', () => {
      wrapper = mount(<UrlWhiteListForm urls={config.entries}
                                        disabled={config.disabled}
                                        update={onUpdate} />);
      expect(wrapper.find('.form-group')).toBeDefined();
      const title = wrapper.find('input#title-input0').at(0);
      expect(title.instance().value).toBe(config.entries[0].title);
    });

    it('should validate and update on input change', () => {
      wrapper = mount(<UrlWhiteListForm urls={config.entries}
                                        disabled={config.disabled}
                                        update={onUpdate} />);
      const title = wrapper.find('input#title-input0').at(0);
      title.instance().value = 'world';
      title.simulate('change');
      expect(onUpdate).toHaveBeenCalled();
    });

    it('should add a new row to the form', () => {
      wrapper = mount(<UrlWhiteListForm urls={config.entries}
                                        disabled={config.disabled}
                                        update={onUpdate} />);
      const button = wrapper.find('button').at(0);
      button.simulate('click');
      const listItems = wrapper.find('tbody>tr');
      expect(listItems.length).toBe(config.entries.length + 1);
    });

    it('should delete a row', () => {
      wrapper = mount(<UrlWhiteListForm urls={config.entries}
                                        disabled={config.disabled}
                                        update={onUpdate} />);
      const deleteButton = wrapper.find('i.fa-trash').at(0);
      deleteButton.simulate('click');
      const listItems = wrapper.find('tbody>tr');
      expect(listItems.length).toBe(config.entries.length - 1);
    });
  });
});
