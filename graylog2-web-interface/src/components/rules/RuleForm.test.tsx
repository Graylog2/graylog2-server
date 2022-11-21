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
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import RuleForm from './RuleForm';
import { PipelineRulesProvider } from './RuleContext';

describe('RuleForm', () => {
  it('should show the previous Description value in the field', () => {
    const ruleToUpdate = {
      source: 'source1',
      description: 'description1',
      title: 'title1',
      created_at: 'created_at1',
      modified_at: 'modified_at1',
    };

    const { getByLabelText } = render(
      <PipelineRulesProvider usedInPipelines={[]} rule={ruleToUpdate}>
        <RuleForm create={false} />
      </PipelineRulesProvider>,
    );

    const descriptionInput = getByLabelText('Description');

    expect(descriptionInput).toBeInTheDocument();

    expect(descriptionInput).toHaveValue(ruleToUpdate.description);
  });
});
