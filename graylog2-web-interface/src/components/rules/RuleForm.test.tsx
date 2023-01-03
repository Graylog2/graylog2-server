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
import userEvent from '@testing-library/user-event';

import RuleForm from './RuleForm';
import { PipelineRulesContext } from './RuleContext';

describe('RuleForm', () => {
  it('should save and update the correct description value', () => {
    const ruleToUpdate = {
      source: `rule "function howto"
      when
        has_field("transaction_date")
      then
        let new_date = parse_date(to_string($message.transaction_date), "yyyy-MM-dd HH:mm:ss");
        set_field("transaction_year", new_date.year);
      end`,
      description: 'description1',
      title: 'title1',
      created_at: 'created_at1',
      modified_at: 'modified_at1',
    };

    const handleDescription = jest.fn();

    const { getByLabelText, getByTitle } = render(
      <PipelineRulesContext.Provider value={{
        description: '',
        handleDescription: handleDescription,
        ruleSource: ruleToUpdate.source,
        handleSavePipelineRule: () => {},
        ruleSourceRef: {},
        usedInPipelines: [],
        onAceLoaded: () => {},
        onChangeSource: () => {},
      }}>
        <RuleForm create={false} />
      </PipelineRulesContext.Provider>,
    );

    const descriptionInput = getByLabelText('Description');

    expect(descriptionInput).toHaveValue('');

    userEvent.paste(descriptionInput, ruleToUpdate.description);
    const createRuleButton = getByTitle('Update rule & close');
    userEvent.click(createRuleButton);

    expect(handleDescription).toHaveBeenCalledWith(ruleToUpdate.description);
  });
});
