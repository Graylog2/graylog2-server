// @flow strict
import React from 'react';
import { mount } from 'wrappedEnzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import 'helpers/mocking/react-dom_mock';
import SavedSearchForm from './SavedSearchForm';

jest.mock('react-overlays', () => ({ Position: mockComponent('MockPosition') }));
jest.mock('react-portal', () => ({ Portal: mockComponent('MockPortal') }));

describe('SavedSearchForm', () => {
  describe('render the SavedSearchForm', () => {
    it('should render create new', () => {
      const wrapper = mount(<SavedSearchForm value="new Title"
                                             onChangeTitle={() => {}}
                                             saveAsSearch={() => {}}
                                             disableCreateNew={false}
                                             toggleModal={() => {}}
                                             isCreateNew
                                             target={() => {}}
                                             saveSearch={() => {}} />);
      expect(wrapper).toMatchSnapshot();
    });

    it('should render save', () => {
      const wrapper = mount(<SavedSearchForm value="new Title"
                                             onChangeTitle={() => {}}
                                             saveAsSearch={() => {}}
                                             disableCreateNew={false}
                                             toggleModal={() => {}}
                                             isCreateNew={false}
                                             target={() => {}}
                                             saveSearch={() => {}} />);
      expect(wrapper).toMatchSnapshot();
    });

    it('should render disabled create new', () => {
      const wrapper = mount(<SavedSearchForm value="new Title"
                                             onChangeTitle={() => {}}
                                             saveAsSearch={() => {}}
                                             disableCreateNew
                                             toggleModal={() => {}}
                                             isCreateNew={false}
                                             target={() => {}}
                                             saveSearch={() => {}} />);
      expect(wrapper).toMatchSnapshot();
    });
  });

  describe('callbacks', () => {
    it('should handle toggleModal', () => {
      const onToggleModal = jest.fn();
      const wrapper = mount(<SavedSearchForm value="new Title"
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
      const wrapper = mount(<SavedSearchForm value="new Title"
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
    const wrapper = mount(<SavedSearchForm value="new Title"
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
    const wrapper = mount(<SavedSearchForm value="new Title"
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
    const wrapper = mount(<SavedSearchForm value="new Title"
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
