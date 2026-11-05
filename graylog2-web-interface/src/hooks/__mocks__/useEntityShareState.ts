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
import type { GRN } from 'logic/permissions/types';

export const ENTITY_SHARE_QUERY_KEY = ['entity-share'] as const;

export const entityShareQueryKey = jest.fn((entityGRN: GRN | null) => [...ENTITY_SHARE_QUERY_KEY, entityGRN ?? 'new']);

const mockSetEntityShareState = jest.fn();

export const useSetEntityShareState = jest.fn(() => mockSetEntityShareState);

const useEntityShareState = jest.fn(() => ({ data: undefined }));

export default useEntityShareState;
