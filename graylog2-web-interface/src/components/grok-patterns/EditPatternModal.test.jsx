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
import React from 'react';
import { mount } from 'wrappedEnzyme';
import 'helpers/mocking/react-dom_mock';

import EditPatternModal from 'components/grok-patterns/EditPatternModal';

describe('<EditPatternModal />', () => {
  it('should render a modal button with as edit', () => {
    const wrapper = mount(<EditPatternModal savePattern={() => {}}
                                            testPattern={() => {}}
                                            validPatternName={() => {}} />);

    expect(wrapper).toExist();
  });

  it('should render a modal button with as create', () => {
    const wrapper = mount(<EditPatternModal create
                                            savePattern={() => {}}
                                            testPattern={() => {}}
                                            validPatternName={() => {}} />);

    expect(wrapper).toExist();
  });
});
