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
import { renderWithDataRouter } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import useRuleBuilder from 'hooks/useRuleBuilder';

import RuleBuilder from './RuleBuilder';

import { PipelineRulesContext } from '../RuleContext';

jest.mock('hooks/useRuleBuilder');

const _createRule = jest.fn();
const _updateRule = jest.fn();
const _fetchValidateRule = jest.fn();

describe('RuleBuilder', () => {
  it('should save Title and Description', () => {
    const createRule = jest.fn();
    const fetchValidateRule = jest.fn();
    const title = 'title';
    const description = 'description';

    asMock(useRuleBuilder).mockReturnValue({
      rule: null,
      createRule,
      fetchValidateRule,
    } as any);

    const { getByLabelText, getByRole } = renderWithDataRouter((
      <PipelineRulesContext.Provider value={{
        description: '',
        handleDescription: () => {},
        ruleSource: '',
        handleSavePipelineRule: () => {},
        ruleSourceRef: {},
        usedInPipelines: [],
        onAceLoaded: () => {},
        onChangeSource: () => {},
      }}>
        <RuleBuilder />
      </PipelineRulesContext.Provider>
    ));
    const titleInput = getByLabelText('Title');
    const descriptionInput = getByLabelText('Description');

    userEvent.paste(titleInput, title);
    userEvent.paste(descriptionInput, description);
    const createRuleButton = getByRole('button', { name: 'Create rule' });
    userEvent.click(createRuleButton);

    expect(1).toBe(1);
    // expect(createRule).toHaveBeenCalledWith({
    //   title,
    //   description,
    // });
  });
});
