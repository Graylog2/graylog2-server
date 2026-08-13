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
import { PluginStore } from 'graylog-web-plugin/plugin';

const EMPTY_STREAMS: Array<string> = [];
const fallbackUseExcludedArchiveStreams = (): Array<string> => EMPTY_STREAMS;

// Streams excluded from archiving under Enterprise > Archive > Configuration.
// The archive plugin provides the actual list; without it (open source) nothing is archived anyway.
const useExcludedArchiveStreams = (): Array<string> => {
  const useExcludedStreams = PluginStore.exports('archive')?.[0]?.useExcludedStreams ?? fallbackUseExcludedArchiveStreams;

  return useExcludedStreams();
};

export default useExcludedArchiveStreams;
