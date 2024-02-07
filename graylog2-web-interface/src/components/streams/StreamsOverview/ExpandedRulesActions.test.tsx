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
import userEvent from '@testing-library/user-event';

import ExpandedRulesActions from 'components/streams/StreamsOverview/ExpandedRulesActions';
import { stream } from 'fixtures/streams';

jest.useFakeTimers();

jest.mock('components/streams/hooks/useStreamRuleTypes', () => () => ({ data: [] }));

describe('ExpandedRulesActions', () => {
  it('should open add rule modal', async () => {
    render(<ExpandedRulesActions stream={stream} />);

    userEvent.click(await screen.findByRole('button', { name: /quick add rule/i }));

    await screen.findByRole('heading', {
      name: /new stream rule/i,
      hidden: true,
    });
  });
});
