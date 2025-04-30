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

export type StageType = {
  stage: number;
  match: 'ALL' | 'EITHER' | 'PASS';
  rules: Array<string>;
};

type ParseError = {
  line: number;
  position_in_line: number;
  type: string;
};

export type PipelineType = {
  id: string;
  title: string;
  description: string;
  source: string;
  created_at: string;
  modified_at: string;
  stages: Array<StageType>;
  errors: Array<ParseError> | null;
  _scope: string;
};

export type NewPipelineType = Pick<PipelineType, 'title' | 'description'> &
  Partial<Pick<PipelineType, 'source' | 'stages'>>;

export const DEFAULT_PIPELINE = {
  id: undefined,
  errors: undefined,
  title: '',
  description: '',
  stages: [{ stage: 0, rules: [], match: 'EITHER' } as StageType],
  source: '',
  created_at: '',
  modified_at: '',
  _scope: 'DEFAULT',
};
