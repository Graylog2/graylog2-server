// @flow strict
import React from 'react';
import renderer from 'react-test-renderer';
import { mountWithTheme as mount } from 'theme/enzymeWithTheme';

import mockComponent from 'helpers/mocking/MockComponent';
import 'helpers/mocking/react-dom_mock';
import BookmarkForm from './BookmarkForm';

jest.mock('react-overlays', () => ({ Position: mockComponent('MockPosition') }));
jest.mock('react-portal', () => ({ Portal: mockComponent('MockPortal') }));

describe('BookmarkForm', () => {
  describe('render the BookmarkForm', () => {
    it('should render create new', () => {
      const wrapper = renderer.create(<BookmarkForm value="new Title"
                                                    onChangeTitle={() => {}}
                                                    saveAsSearch={() => {}}
                                                    disableCreateNew={false}
                                                    toggleModal={() => {}}
                                                    isCreateNew
                                                    target={() => {}}
                                                    saveSearch={() => {}} />);
      expect(wrapper.toJSON()).toMatchSnapshot();
    });

    it('should render save', () => {
      const wrapper = renderer.create(<BookmarkForm value="new Title"
                                                    onChangeTitle={() => {}}
                                                    saveAsSearch={() => {}}
                                                    disableCreateNew={false}
                                                    toggleModal={() => {}}
                                                    isCreateNew={false}
                                                    target={() => {}}
                                                    saveSearch={() => {}} />);
      expect(wrapper.toJSON()).toMatchSnapshot();
    });

    it('should render disabled create new', () => {
      const wrapper = renderer.create(<BookmarkForm value="new Title"
                                                    onChangeTitle={() => {}}
                                                    saveAsSearch={() => {}}
                                                    disableCreateNew
                                                    toggleModal={() => {}}
                                                    isCreateNew={false}
                                                    target={() => {}}
                                                    saveSearch={() => {}} />);
      expect(wrapper.toJSON()).toMatchSnapshot();
    });
  });

  describe('callbacks', () => {
    it('should handle toggleModal', () => {
      const onToggleModal = jest.fn();
      const wrapper = mount(<BookmarkForm value="new Title"
                                          onChangeTitle={() => {}}
                                          saveAsSearch={() => {}}
                                          disableCreateNew
                                          toggleModal={onToggleModal}
                                          isCreateNew={false}
                                          target={() => {}}
                                          saveSearch={() => {}} />);
      wrapper.find('button[children="Cancel"]').simulate('click');
      expect(onToggleModal).toHaveBeenCalledTimes(1);
    });

    it('should handle saveSearch', () => {
      const onSave = jest.fn();
      const wrapper = mount(<BookmarkForm value="new Title"
                                          onChangeTitle={() => {}}
                                          saveAsSearch={() => {}}
                                          disableCreateNew
                                          toggleModal={() => {}}
                                          isCreateNew={false}
                                          target={() => {}}
                                          saveSearch={onSave} />);
      wrapper.find('button[children="Save"]').simulate('click');
      expect(onSave).toHaveBeenCalledTimes(1);
    });
  });

  it('should handle saveAsSearch', () => {
    const onSaveAs = jest.fn();
    const wrapper = mount(<BookmarkForm value="new Title"
                                        onChangeTitle={() => {}}
                                        saveAsSearch={onSaveAs}
                                        disableCreateNew={false}
                                        toggleModal={() => {}}
                                        isCreateNew={false}
                                        target={() => {}}
                                        saveSearch={() => {}} />);
    wrapper.find('button[children="Save as"]').simulate('click');
    expect(onSaveAs).toHaveBeenCalledTimes(1);
  });

  it('should not handle saveAsSearch if disabled', () => {
    const onSaveAs = jest.fn();
    const wrapper = mount(<BookmarkForm value="new Title"
                                        onChangeTitle={() => {}}
                                        saveAsSearch={onSaveAs}
                                        disableCreateNew
                                        toggleModal={() => {}}
                                        isCreateNew={false}
                                        target={() => {}}
                                        saveSearch={() => {}} />);
    wrapper.find('button[children="Save as"]').simulate('click');
    expect(onSaveAs).toHaveBeenCalledTimes(0);
  });

  it('should handle create new', () => {
    const onSaveAs = jest.fn();
    const wrapper = mount(<BookmarkForm value="new Title"
                                        onChangeTitle={() => {}}
                                        saveAsSearch={onSaveAs}
                                        disableCreateNew={false}
                                        toggleModal={() => {}}
                                        isCreateNew
                                        target={() => {}}
                                        saveSearch={() => {}} />);
    wrapper.find('button[children="Create new"]').simulate('click');
    expect(onSaveAs).toHaveBeenCalledTimes(1);
  });
});
