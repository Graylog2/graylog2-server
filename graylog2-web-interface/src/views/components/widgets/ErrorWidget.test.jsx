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
