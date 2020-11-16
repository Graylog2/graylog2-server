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

import PossiblyHighlight from './PossiblyHighlight';

describe('PossiblyHighlight', () => {
  it('renders something for an `undefined` field value', () => {
    const wrapper = mount(<PossiblyHighlight field="foo" value={undefined} />);

    expect(wrapper).not.toBeNull();
  });

  it('renders something for a `null` field value', () => {
    const wrapper = mount(<PossiblyHighlight field="foo" value={null} />);

    expect(wrapper).not.toBeNull();
  });

  it('renders for invalid highlighting ranges', () => {
    const wrapper = mount(<PossiblyHighlight field="foo" value="bar" highlightRanges={undefined} />);

    expect(wrapper).not.toBeNull();
  });
});
