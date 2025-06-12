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
import { render, screen, waitFor, fireEvent } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import type { Optional } from 'utility-types';

import OriginalTokenList from 'components/users/TokenList';
import { alice, serviceUser } from 'fixtures/users';
import useDeleteTokenMutation from 'components/users/UsersTokenManagement/hooks/useDeleteTokenMutation';
import asMock from 'helpers/mocking/AsMock';

jest.mock('components/common/ClipboardButton', () => 'clipboard-button');

const tokens = [
  {
    name: 'Acme',
    token: 'beef2001',
    id: 'abc1',
    last_access: '2020-12-08T16:46:00Z',
    created_at: '2020-12-08T00:00:00Z',
    expires_at: '2022-01-01T00:00:00Z',
    tokenTtl: 'P30D',
  },
  {
    name: 'Hamfred',
    token: 'beef2002',
    id: 'abc2',
    last_access: '1970-01-01T00:00:00.000Z',
    created_at: '1970-01-01T00:00:00Z',
    expires_at: '2022-01-01T00:00:00Z',
    tokenTtl: 'PT48H',
  },
];
const TokenList = (props: Optional<React.ComponentProps<typeof OriginalTokenList>, 'onCreate'>) => (
  <OriginalTokenList onCreate={async () => tokens[0]} {...props} />
);

const deleteToken = jest.fn(() => Promise.resolve());

jest.mock('components/users/UsersTokenManagement/hooks/useDeleteTokenMutation', () => ({
  __esModule: true,
  default: jest.fn(() => ({ deleteToken: jest.fn(() => Promise.resolve()) })),
}));

describe('<TokenList />', () => {
  beforeAll(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2020-12-09T17:42:00Z'));
  });

  beforeEach(() => {
    asMock(useDeleteTokenMutation).mockImplementation(() => ({ deleteToken }));
  });

  afterAll(() => {
    jest.useRealTimers();
  });

  it('should render with empty tokens', async () => {
    render(<TokenList tokens={[]} user={alice} />);
    await screen.findByText(/no tokens to display./i);
  });

  it('should render with tokens', async () => {
    render(<TokenList tokens={tokens} user={alice} />);
    await screen.findByText(/acme/i);
  });

  it('should add new token and display it', async () => {
    const createFn = jest.fn(() =>
      Promise.resolve({
        name: 'hans',
        token: 'beef2003',
        id: 'abc3',
        last_access: '1970-01-01T00:00:00.000Z',
        expires_at: '2020-01-01T00:00:00.000Z',
        tokenTtl: 'PT72H',
      }),
    );

    render(<TokenList tokens={tokens} onCreate={createFn} user={alice} />);

    const nameInput = await screen.findByPlaceholderText('What is this token for?');
    await userEvent.type(nameInput, 'hans');

    const ttlInput = await screen.findByLabelText('Token TTL');
    fireEvent.change(ttlInput, { target: { value: 'PT72H' } });

    const createToken = await screen.findByRole('button', { name: 'Create Token' });
    await userEvent.click(createToken);

    await screen.findByText('beef2003');

    await screen.findByText(/This is your new token./i);

    await waitFor(() => {
      expect(createFn).toHaveBeenCalledWith({ 'tokenName': 'hans', 'tokenTtl': 'PT72H' });
    });
  });

  it('should add new token for service account', async () => {
    const createFn = jest.fn(() =>
      Promise.resolve({
        name: 'hans',
        token: 'beef2003',
        id: 'abc3',
        last_access: '1970-01-01T00:00:00.000Z',
        expires_at: '2020-01-01T00:00:00.000Z',
        tokenTtl: 'P100Y',
      }),
    );

    render(<TokenList tokens={tokens} onCreate={createFn} user={serviceUser} />);

    const nameInput = await screen.findByPlaceholderText('What is this token for?');
    await userEvent.type(nameInput, 'hans');

    const createToken = await screen.findByRole('button', { name: 'Create Token' });
    await userEvent.click(createToken);

    await screen.findByText('beef2003');

    await waitFor(() => {
      expect(createFn).toHaveBeenCalledWith({ 'tokenName': 'hans', 'tokenTtl': 'P100Y' });
    });
  });

  it('should delete a token', async () => {
    const onDeleteFn = jest.fn();
    render(<TokenList tokens={tokens} user={alice} onDelete={onDeleteFn} />);

    const deleteButtons = await screen.findAllByRole('button', { name: 'Delete' });
    await userEvent.click(deleteButtons[0]);
    await screen.findByRole('heading', {
      name: /deleting token/i,
    });

    await userEvent.click(await screen.findByRole('button', { name: /Confirm/i }));

    await waitFor(() => {
      expect(deleteToken).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(onDeleteFn).toHaveBeenCalled();
    });
  });

  it('show include token last access time', async () => {
    render(<TokenList tokens={tokens} user={alice} />);

    await screen.findByText('Never used');

    await screen.findByText(/a day ago/i);
  });
});
