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
import { render, screen } from 'wrappedTestingLibrary';
import { fireEvent } from '@testing-library/react';

import FieldType from 'views/logic/fieldtypes/FieldType';

import PivotConfiguration from './PivotConfiguration';

describe('PivotConfiguration', () => {
  it('stops submit event propagation', () => {
    const onSubmit = jest.fn((e) => e.persist());

    render((
      <div onSubmit={onSubmit}>
        <PivotConfiguration type={FieldType.create('terms')} config={{ limit: 3 }} onClose={jest.fn()} />
      </div>
    ));

    const done = screen.getByRole('button', 'Done');
    fireEvent.click(done);

    expect(onSubmit).not.toHaveBeenCalled();
  });
});
