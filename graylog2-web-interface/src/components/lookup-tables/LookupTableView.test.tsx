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
import { mount } from 'wrappedEnzyme';

import {
  createLookupTable,
  createLookupTableCache,
  createLookupTableAdapter,
} from 'fixtures/lookupTables';
import { asMock } from 'helpers/mocking';
import useScopePermissions from 'hooks/useScopePermissions';
import type { GenericEntityType } from 'logic/lookup-tables/types';

import LookupTableView from './LookupTableView';

jest.mock('hooks/useScopePermissions');

const renderedLUT = (scope: string) => {
  const table = createLookupTable(1, { _scope: scope });
  const cache = createLookupTableCache();
  const dataAdapter = createLookupTableAdapter();

  return render(<LookupTableView table={table} cache={cache} dataAdapter={dataAdapter} />);
};

const mountLUT = (scope: string) => {
  const table = createLookupTable(1, { _scope: scope });
  const cache = createLookupTableCache();
  const dataAdapter = createLookupTableAdapter();

  return mount(<LookupTableView table={table} cache={cache} dataAdapter={dataAdapter} />);
};

describe('LookupTableView', () => {
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

  it('should show "edit" button', () => {
    renderedLUT('DEFAULT');

    expect(screen.getByRole('button', { name: /edit/i })).toBeInTheDocument();
  });

  it('should not show "edit" button', () => {
    renderedLUT('ILLUMINATE');

    expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument();
  });

  it('should allow non-word-characters in lookup tables key field', () => {
    const nonWordCharactersKey = '177.228.126.200';

    const wrapper = mountLUT('DEFAULT');

    wrapper.find('input[name="lookupkey"]').simulate('change', { target: { name: 'lookupkey', value: nonWordCharactersKey } });

    expect(wrapper.find('input[name="lookupkey"]').length).toBe(1);
    expect(wrapper.find('input[name="lookupkey"]').prop('value')).toBe(nonWordCharactersKey);

    expect(wrapper.find('button[name="lookupbutton"]').prop('disabled')).toBe(false);
  });
});
