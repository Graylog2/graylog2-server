// @flow strict
import React from 'react';
import { mount } from 'wrappedEnzyme';

import SearchError from 'views/logic/SearchError';
import ErrorWidget from './ErrorWidget';

describe('<ErrorWidget />', () => {
  it('should display a list item for every provided error', () => {
    const errors = [
      new SearchError({ description: 'The first error' }),
      new SearchError({ description: 'The second error' }),
    ];

    const wrapper = mount(<ErrorWidget errors={errors} />);
    const firstListItem = wrapper.find('li').at(0);
    const secondListItem = wrapper.find('li').at(1);

    expect(firstListItem.text()).toContain(errors[0].description);
    expect(secondListItem.text()).toContain(errors[1].description);
  });
});
