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
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import TypeSpecificValue from 'views/components/TypeSpecificValue';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import useFeature from 'hooks/useFeature';

jest.mock('hooks/useFeature');

describe('SearchResult', () => {
  beforeEach(() => {
    asMock(useFeature).mockReturnValue(true);
  });

  it('should render prettified value if unit defined', async () => {
    render(
      <TypeSpecificValue field="field1" value={6543.21} unit={FieldUnit.fromJSON({ abbrev: 'ms', unit_type: 'time' })} />,
    );

    await screen.findByText('6.5 s');
  });

  it('should render original value if unit not defined', async () => {
    render(
      <TypeSpecificValue field="field1" value={6543.21} />,
    );

    await screen.findByText(6543.21);
  });
});
