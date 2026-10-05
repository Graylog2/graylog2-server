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
import type { Stream } from 'logic/streams/types';
import useAllStreams from 'components/streams/hooks/useAllStreams';

import asMock from './AsMock';

/**
 * Mocks the `useAllStreams` hook. Requires `jest.mock('components/streams/hooks/useAllStreams')`
 * in the test file, since `jest.mock` calls are hoisted per module.
 */
const mockUseAllStreams = (streams: Array<Stream> | undefined = []) =>
  asMock(useAllStreams).mockReturnValue({ data: streams, isLoading: false, refetch: jest.fn() });

export default mockUseAllStreams;
