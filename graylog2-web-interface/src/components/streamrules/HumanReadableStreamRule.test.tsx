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
import { render, screen, act } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import useStreamRuleTypes from 'components/streams/hooks/useStreamRuleTypes';
import { streamRuleTypes } from 'fixtures/streamRuleTypes';
import { changeLanguage } from 'i18n';

import HumanReadableStreamRule from './HumanReadableStreamRule';

jest.mock('components/streams/hooks/useStreamRuleTypes');

describe('<HumanReadableStreamRule />', () => {
  beforeEach(() => {
    asMock(useStreamRuleTypes).mockReturnValue({ data: streamRuleTypes });
  });

  afterEach(() => changeLanguage('en'));

  it('translates the description of the rule type', async () => {
    render(
      <HumanReadableStreamRule
        streamRule={{ type: 1, field: 'source', value: 'example.org', inverted: false }}
        inputs={[]}
      />,
    );

    await screen.findByText(/match exactly/);

    await act(() => changeLanguage('de'));

    await screen.findByText(/exakt übereinstimmen mit/);
  });
});
