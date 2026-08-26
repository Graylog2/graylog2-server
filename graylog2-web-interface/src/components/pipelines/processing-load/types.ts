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
export type PipelineLoadEntry = {
  pipeline_id: string;
  load_percent: number;
};

export type RuleLoadEntry = {
  rule_id: string;
  load_percent: number;
};

export type StageRuleLoadEntry = {
  pipeline_id: string;
  rule_id: string;
  stage: number;
  load_percent: number;
  pipeline_share_percent: number;
};

export type ProcessingLoadResponse = {
  available: boolean;
  total_cost_microseconds_per_second: number;
  pipelines: Array<PipelineLoadEntry>;
  rules: Array<RuleLoadEntry>;
  stage_rules: Array<StageRuleLoadEntry>;
};
