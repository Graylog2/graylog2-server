import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import RuleForm from './RuleForm';
import { PipelineRulesProvider } from './RuleContext';

describe('RuleForm', () => {
  it('should show the previous Description value in the field', () => {
    const ruleToUpdate = {
      source: 'source1',
      description: 'description1',
    };

    const { getByLabelText } = render(
      <PipelineRulesProvider usedInPipelines={[]} rule={ruleToUpdate}>
        <RuleForm create={false} />
      </PipelineRulesProvider>,
    );

    const descriptionInput = getByLabelText('Description');

    expect(descriptionInput).toBeInTheDocument();

    expect(descriptionInput.value).toBe(ruleToUpdate.description);
  });
});
