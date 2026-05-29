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
import type { PipelineLoadEntry, ProcessingLoadResponse, RuleLoadEntry, StageRuleLoadEntry } from './types';

export const lookupPipeline = (
  response: ProcessingLoadResponse | undefined,
  pipelineId: string,
): PipelineLoadEntry | undefined => response?.pipelines.find((entry) => entry.pipeline_id === pipelineId);

export const lookupRule = (response: ProcessingLoadResponse | undefined, ruleId: string): RuleLoadEntry | undefined =>
  response?.rules.find((entry) => entry.rule_id === ruleId);

export const lookupStageRule = (
  response: ProcessingLoadResponse | undefined,
  pipelineId: string,
  ruleId: string,
  stage: number,
): StageRuleLoadEntry | undefined =>
  response?.stage_rules.find(
    (entry) => entry.pipeline_id === pipelineId && entry.rule_id === ruleId && entry.stage === stage,
  );

const isLoadable = (response: ProcessingLoadResponse | undefined): response is ProcessingLoadResponse =>
  !!response && response.available && response.total_cost_microseconds_per_second > 0;

export const getPipelineLoadPercent = (
  response: ProcessingLoadResponse | undefined,
  pipelineId: string,
): number | undefined => (isLoadable(response) ? lookupPipeline(response, pipelineId)?.load_percent : undefined);

export const getRuleLoadPercent = (response: ProcessingLoadResponse | undefined, ruleId: string): number | undefined =>
  isLoadable(response) ? lookupRule(response, ruleId)?.load_percent : undefined;

export const getStageRuleLoadPercent = (
  response: ProcessingLoadResponse | undefined,
  pipelineId: string,
  ruleId: string,
  stage: number,
): number | undefined =>
  isLoadable(response) ? lookupStageRule(response, pipelineId, ruleId, stage)?.load_percent : undefined;

export const getStageRulePipelineSharePercent = (
  response: ProcessingLoadResponse | undefined,
  pipelineId: string,
  ruleId: string,
  stage: number,
): number | undefined =>
  isLoadable(response) ? lookupStageRule(response, pipelineId, ruleId, stage)?.pipeline_share_percent : undefined;
