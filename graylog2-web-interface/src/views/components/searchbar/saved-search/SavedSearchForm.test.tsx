/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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

      expect(wrapper).toExist();
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

      expect(wrapper).toExist();
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

      expect(wrapper).toExist();
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
