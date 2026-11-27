/*
 * Copyright (C) 2026 Graylog, Inc.
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
import userEvent from '@testing-library/user-event';

import type { Decorator } from 'views/components/messagelist/decorators/Types';

import DecoratorsConfigUpdate from './DecoratorsConfigUpdate';

const decorator = (id: string, order: number): Decorator => ({ id, type: 'dummy', order, stream: null, config: {} });

describe('DecoratorsConfigUpdate', () => {
  const renderUpdate = (decorators: Array<Decorator>, show: boolean, onSave: (d: Array<Decorator>) => unknown) => (
    <DecoratorsConfigUpdate
      streams={[]}
      types={{}}
      decorators={decorators}
      show={show}
      onCancel={() => {}}
      onSave={onSave}
    />
  );

  it('shows and saves the current decorators when it is opened again after a save', async () => {
    const onSave = jest.fn();
    const initial = [decorator('a', 0)];
    const refetched = [decorator('a', 0), decorator('b', 1)];

    const { rerender } = render(renderUpdate(initial, true, onSave));

    await userEvent.click(await screen.findByRole('button', { name: /update configuration/i }));

    expect(onSave).toHaveBeenLastCalledWith(initial);

    rerender(renderUpdate(refetched, false, onSave));
    rerender(renderUpdate(refetched, true, onSave));

    await userEvent.click(await screen.findByRole('button', { name: /update configuration/i }));

    expect(onSave).toHaveBeenLastCalledWith(refetched);
  });
});
