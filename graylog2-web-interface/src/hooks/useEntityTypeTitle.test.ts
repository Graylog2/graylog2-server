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
import { renderHook } from 'wrappedTestingLibrary/hooks';

import usePluginEntities from 'hooks/usePluginEntities';
import { asMock } from 'helpers/mocking';

import useEntityTypeTitle from './useEntityTypeTitle';

jest.mock('hooks/usePluginEntities');

const renderTitle = (type: string) => renderHook(() => useEntityTypeTitle(type)).result.current;

describe('useEntityTypeTitle', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([{ type: 'plugin_entity', title: 'plugin entity' }]);
  });

  it('returns a readable title for a collection', () => {
    expect(renderTitle('collection')).toBe('collection');
  });

  it('returns a readable title for collection entities, which is what sharing a collection records', () => {
    expect(renderTitle('collection_entities')).toBe('collection entities');
  });

  it('returns titles registered by plugins', () => {
    expect(renderTitle('plugin_entity')).toBe('plugin entity');
  });

  it('returns undefined for unknown types', () => {
    expect(renderTitle('unknown_type')).toBeUndefined();
  });
});
