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
import {
  getPipelineLoadPercent,
  getRuleLoadPercent,
  getStageRuleLoadPercent,
  getStageRulePipelineSharePercent,
  lookupPipeline,
  lookupRule,
  lookupStageRule,
} from './lookups';
import type { ProcessingLoadResponse } from './types';

const baseResponse: ProcessingLoadResponse = {
  available: true,
  total_cost_microseconds_per_second: 100,
  pipelines: [
    { pipeline_id: 'p1', load_percent: 60 },
    { pipeline_id: 'p2', load_percent: 0 },
  ],
  rules: [{ rule_id: 'r1', load_percent: 40 }],
  stage_rules: [
    { pipeline_id: 'p1', rule_id: 'r1', stage: 0, load_percent: 25, pipeline_share_percent: 60 },
    { pipeline_id: 'p1', rule_id: 'r1', stage: 1, load_percent: 15, pipeline_share_percent: 40 },
  ],
};

describe('processing-load lookups', () => {
  describe('lookupPipeline / lookupRule / lookupStageRule', () => {
    it('finds a pipeline by id', () => {
      expect(lookupPipeline(baseResponse, 'p1')?.load_percent).toBe(60);
    });

    it('finds a rule by id', () => {
      expect(lookupRule(baseResponse, 'r1')?.load_percent).toBe(40);
    });

    it('returns undefined for unknown ids', () => {
      expect(lookupPipeline(baseResponse, 'missing')).toBeUndefined();
      expect(lookupRule(baseResponse, 'missing')).toBeUndefined();
    });

    it('disambiguates stage-rule entries by (pipelineId, ruleId, stage)', () => {
      expect(lookupStageRule(baseResponse, 'p1', 'r1', 0)?.load_percent).toBe(25);
      expect(lookupStageRule(baseResponse, 'p1', 'r1', 1)?.load_percent).toBe(15);
    });

    it('returns undefined for stage-rule with mismatched stage', () => {
      expect(lookupStageRule(baseResponse, 'p1', 'r1', 99)).toBeUndefined();
    });

    it('returns undefined for an undefined response', () => {
      expect(lookupPipeline(undefined, 'p1')).toBeUndefined();
      expect(lookupRule(undefined, 'r1')).toBeUndefined();
      expect(lookupStageRule(undefined, 'p1', 'r1', 0)).toBeUndefined();
    });
  });

  describe('getXLoadPercent', () => {
    it('returns the load_percent for a known entry', () => {
      expect(getPipelineLoadPercent(baseResponse, 'p1')).toBe(60);
      expect(getRuleLoadPercent(baseResponse, 'r1')).toBe(40);
      expect(getStageRuleLoadPercent(baseResponse, 'p1', 'r1', 0)).toBe(25);
    });

    it('returns 0 for participating zero-cost rows when denominator is non-zero', () => {
      expect(getPipelineLoadPercent(baseResponse, 'p2')).toBe(0);
    });

    it('returns undefined for missing entries', () => {
      expect(getPipelineLoadPercent(baseResponse, 'unknown')).toBeUndefined();
    });

    it('returns undefined when the response is unavailable', () => {
      const unavailable: ProcessingLoadResponse = { ...baseResponse, available: false };

      expect(getPipelineLoadPercent(unavailable, 'p1')).toBeUndefined();
    });

    it('returns undefined when total_cost is zero (denominator-zero)', () => {
      const zero: ProcessingLoadResponse = { ...baseResponse, total_cost_microseconds_per_second: 0 };

      expect(getPipelineLoadPercent(zero, 'p1')).toBeUndefined();
      expect(getRuleLoadPercent(zero, 'r1')).toBeUndefined();
      expect(getStageRuleLoadPercent(zero, 'p1', 'r1', 0)).toBeUndefined();
    });

    it('returns undefined for an undefined response', () => {
      expect(getPipelineLoadPercent(undefined, 'p1')).toBeUndefined();
    });
  });

  describe('getStageRulePipelineSharePercent', () => {
    it('returns the pipeline_share_percent for a known stage-rule', () => {
      expect(getStageRulePipelineSharePercent(baseResponse, 'p1', 'r1', 0)).toBe(60);
      expect(getStageRulePipelineSharePercent(baseResponse, 'p1', 'r1', 1)).toBe(40);
    });

    it('returns undefined for missing stage-rule entries', () => {
      expect(getStageRulePipelineSharePercent(baseResponse, 'p1', 'r1', 99)).toBeUndefined();
    });

    it('returns undefined when the response is unavailable or denominator-zero', () => {
      expect(getStageRulePipelineSharePercent({ ...baseResponse, available: false }, 'p1', 'r1', 0)).toBeUndefined();
      expect(
        getStageRulePipelineSharePercent({ ...baseResponse, total_cost_microseconds_per_second: 0 }, 'p1', 'r1', 0),
      ).toBeUndefined();
    });

    it('returns undefined for an undefined response', () => {
      expect(getStageRulePipelineSharePercent(undefined, 'p1', 'r1', 0)).toBeUndefined();
    });
  });
});
