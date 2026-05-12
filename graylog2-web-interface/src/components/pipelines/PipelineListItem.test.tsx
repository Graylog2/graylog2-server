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
import type { PipelineType } from 'components/pipelines/types';

import getUsedStages from './PipelineStages';

const pipelineWithStages = (stages: Array<number>): PipelineType => ({
  id: 'pipeline-1',
  title: 'Pipeline',
  description: 'Pipeline description',
  source: '',
  created_at: '2024-01-01T00:00:00.000Z',
  modified_at: '2024-01-01T00:00:00.000Z',
  stages: stages.map((stage) => ({ stage, match: 'EITHER', rules: [] })),
  errors: null,
  has_deprecated_functions: false,
  _scope: 'DEFAULT',
});

describe('PipelineListItem', () => {
  it('sorts used stages numerically', () => {
    expect(getUsedStages([pipelineWithStages([1, 100]), pipelineWithStages([3, 2])])).toEqual([1, 2, 3, 100]);
  });

  it('removes duplicate used stages', () => {
    expect(getUsedStages([pipelineWithStages([2, 1]), pipelineWithStages([1, 2])])).toEqual([1, 2]);
  });
});
