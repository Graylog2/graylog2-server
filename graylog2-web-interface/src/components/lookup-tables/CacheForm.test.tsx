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
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import { buildLookupTableCache } from 'fixtures/lookupTables';
import useScopePermissions from 'hooks/useScopePermissions';
import type { GenericEntityType } from 'logic/lookup-tables/types';
import { asMock } from 'helpers/mocking';

import CaffeineCacheFieldSet from './caches/CaffeineCacheFieldSet';
import CacheForm from './CacheForm';
import NullCacheFieldSet from './caches/NullCacheFieldSet';

jest.mock('hooks/useScopePermissions');

PluginStore.register(new PluginManifest({}, {
  lookupTableCaches: [
    {
      type: 'guava_cache', // old name kept for backwards compatibility
      displayName: 'Node-local, in-memory cache',
      formComponent: CaffeineCacheFieldSet,
    },
    {
      type: 'none',
      displayName: 'Do not cache values',
      formComponent: NullCacheFieldSet,
    },
  ],
}));

const renderedCache = ({
  scope,
  inCache = { ...buildLookupTableCache() },
  create = false,
  withConfig = true,
  // eslint-disable-next-line no-console
  validate = () => { console.log('validation called'); },
  // eslint-disable-next-line no-console
  saved = () => { console.log('saved called'); },
  validationErrors = {},
}) => {
  const mockCache = {
    ...inCache,
    _scope: scope,
  };

  if (!withConfig) mockCache.config = { type: 'none' };

  return render(
    <CacheForm cache={mockCache}
               type={mockCache.config.type}
               saved={saved}
               title="Data Cache"
               create={create}
               validate={validate}
               validationErrors={validationErrors} />,
  );
};

describe('CacheForm', () => {
  beforeAll(() => {
    asMock(useScopePermissions).mockImplementation(
      (entity: GenericEntityType) => {
        const scopes = {
          ILLUMINATE: { is_mutable: false },
          DEFAULT: { is_mutable: true },
        };

        return {
          loadingScopePermissions: false,
          scopePermissions: scopes[entity._scope],
        };
      },
    );
  });

  it('should show "Update Cache" button', async () => {
    renderedCache({ scope: 'DEFAULT' });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /update/i })).toBeInTheDocument();
    });
  });

  it('should not show "Update Cache" button', async () => {
    renderedCache({ scope: 'ILLUMINATE' });

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /update/i })).not.toBeInTheDocument();
    });
  });

  it('should show required error message', async () => {
    const cache = buildLookupTableCache(1, {
      title: '',
      description: '',
      name: '',
    });

    renderedCache({ scope: 'DEFAULT', inCache: cache, withConfig: false });

    const titleInput = screen.queryByLabelText('* Title');
    fireEvent.blur(titleInput);
    const requiredErrorMessages = await screen.findAllByText('Required');

    expect(requiredErrorMessages.length).toBeGreaterThanOrEqual(2);
  });

  it('should show duplicated name error', async () => {
    const cache = buildLookupTableCache(1, {
      title: 'A duplicated name',
      description: '',
      name: 'a-duplicated-name',
    });

    renderedCache({
      scope: 'DEFAULT',
      inCache: cache,
      create: true,
      withConfig: false,
      validationErrors: { name: ['The cache name is already in use.'] },
    });

    const titleInput = screen.queryByLabelText('* Title');
    fireEvent.blur(titleInput);

    const dupNameError = await screen.findByText('The cache name is already in use.');

    expect(dupNameError).toBeVisible();
  });

  it('should not submit invalid form', async () => {
    const cache = buildLookupTableCache(1, {
      title: 'another-test-cache',
      description: '',
      name: 'another-test-cache',
    });

    const mockSaved = jest.fn();

    renderedCache({
      scope: 'DEFAULT',
      inCache: cache,
      withConfig: false,
      saved: mockSaved,
    });

    const titleEle = await screen.findByLabelText('* Title');
    const nameEle = await screen.findByLabelText('* Name');
    const submitButton = await screen.findByText('Update Cache');

    fireEvent.change(titleEle, { target: { value: '' } });
    fireEvent.change(nameEle, { target: { value: '' } });
    userEvent.click(submitButton);

    await waitFor(() => {
      expect(mockSaved).not.toHaveBeenCalled();
    });
  });

  it('should allow user to submit a valid form', async () => {
    const cache = buildLookupTableCache(1, {
      title: '',
      description: '',
      name: '',
    });

    const mockSaved = jest.fn();

    renderedCache({
      scope: 'DEFAULT',
      inCache: cache,
      withConfig: false,
      saved: mockSaved,
    });

    const titleEle = await screen.findByLabelText('* Title');
    const nameEle = await screen.findByLabelText('* Name');
    const submitButton = await screen.findByText('Update Cache');

    fireEvent.change(titleEle, { target: { value: 'Test title' } });
    fireEvent.change(nameEle, { target: { value: 'test-title' } });
    userEvent.click(submitButton);

    await waitFor(() => {
      expect(mockSaved).toHaveBeenCalled();
    });
  });
});
