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
import { render, waitFor } from 'wrappedTestingLibrary';
import { BrowserRouter as Router } from 'react-router-dom';

import { TABLE, CACHE, DATA_ADAPTER } from './fixtures';
import LookupTableView from './LookupTableView';

const renderedLUT = (scope: string) => {
  TABLE._metadata = {
    scope: scope,
    revision: 2,
    created_at: '2022-06-13T08:47:12Z',
    updated_at: '2022-06-29T12:00:28Z',
  };

  return render(
    <Router>
      <LookupTableView table={TABLE} cache={CACHE} dataAdapter={DATA_ADAPTER} />
    </Router>,
  );
};

describe('LookupTableView', () => {
  it('should show "edit" button', async () => {
    const { getByAltText } = renderedLUT('DEFAULT');

    let actionBtn = null;
    await waitFor(() => { actionBtn = getByAltText('edit button'); });

    expect(actionBtn).toBeVisible();
  });

  it('should not show "edit" button', async () => {
    const { queryByAltText } = renderedLUT('ILLUMINATE');

    let actionBtn = null;
    await waitFor(() => { actionBtn = queryByAltText('edit button'); });

    expect(actionBtn).toBeNull();
  });
});
