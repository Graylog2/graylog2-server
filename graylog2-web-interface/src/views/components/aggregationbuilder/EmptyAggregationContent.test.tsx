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
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';

import EmptyAggregationContent from './EmptyAggregationContent';

import useRenderCompletionCallback from '../widgets/useRenderCompletionCallback';

jest.mock('../widgets/useRenderCompletionCallback');

describe('EmptyAggregationContext', () => {
  it('calls render completion callback after first render', () => {
    const onRenderComplete = jest.fn();

    asMock(useRenderCompletionCallback).mockReturnValue(onRenderComplete);

    render(<EmptyAggregationContent toggleEdit={() => {}} editing={false} />);

    expect(onRenderComplete).toHaveBeenCalled();
  });
});
