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
export type InputPipelineRule = {
  id: string;
  pipeline: string;
  pipeline_id: string;
  rule: string;
  rule_id: string;
  stage: number;
  connected_streams: Array<{ id: string; title: string }>;
};

export type InputStreamRule = {
  id: string;
  stream_id: string;
  stream: string;
  rule_field: string;
  rule_type: number;
  rule_value: string;
  inverted: boolean;
  description: string;
};
