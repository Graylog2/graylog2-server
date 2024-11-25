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

export type IndexRange = {
  index_name: string,
  begin: number,
  end: number,
  is_warm_tiered: boolean,
  stream_names: Array<string>
}

export type StreamDataRouting = {
  stream_name: string,
  stream_id: string,
  destination: string,
  from: string,
  to: string,
}

export type QueryValidationState = {
  status: 'OK' | 'ERROR' | 'WARNING' | 'INFO',
  explanations: Array<{
    id: string,
    errorType: string,
    errorTitle: string,
    errorMessage: string,
    beginLine: number,
    endLine: number,
    beginColumn: number,
    endColumn: number,
    relatedProperty?: string,
  }>,
  context: {
    searched_index_ranges: Array<IndexRange>,
    data_routed_streams?: Array<StreamDataRouting>,
    searched_time_range?: {
      from: string,
      to: string,
      type: 'absolute',
    },
  }
};
