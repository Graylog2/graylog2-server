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
import { jsonifyText } from './helpers';

import { PipelineRulesContext } from '../RuleContext';

jest.mock('hooks/useRuleBuilder');

describe('RuleBuilder', () => {
  beforeAll(() => {
    jest.spyOn(console, 'error').mockImplementation((message) => {
      if (!JSON.stringify(message || '').includes('Warning: validateDOMNesting')) {
        // eslint-disable-next-line no-console
        console.error(message);
      }
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should save Title and Description', () => {
    const createRule = jest.fn();
    const title = 'title';
    const description = 'description';
    const rule_builder = { actions: [], conditions: [], operator: 'AND' };

    asMock(useRuleBuilder).mockReturnValue({
      rule: null,
      createRule,
    } as any);

    const { getByLabelText, getByRole } = renderWithDataRouter((
      <PipelineRulesContext.Provider value={{
        simulateRule: () => {},
        setRawMessageToSimulate: () => {},
        setRuleSimulationResult: () => {},
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

    expect(createRule).toHaveBeenCalledWith({
      title,
      description,
      rule_builder,
    });
  });

  it('should update Title and Description', () => {
    const updateRule = jest.fn();
    const title = 'title';
    const description = 'description';
    const rule_builder = { actions: [], conditions: [], operator: 'AND' };

    asMock(useRuleBuilder).mockReturnValue({
      rule: { title: '', description: '', rule_builder },
      updateRule,
    } as any);

    const { getByLabelText, getByRole } = renderWithDataRouter((
      <PipelineRulesContext.Provider value={{
        simulateRule: () => {},
        setRawMessageToSimulate: () => {},
        setRuleSimulationResult: () => {},
      }}>
        <RuleBuilder />
      </PipelineRulesContext.Provider>
    ));
    const titleInput = getByLabelText('Title');
    const descriptionInput = getByLabelText('Description');

    userEvent.paste(titleInput, title);
    userEvent.paste(descriptionInput, description);
    const updateRuleButton = getByRole('button', { name: 'Update rule' });
    userEvent.click(updateRuleButton);

    expect(updateRule).toHaveBeenCalledWith({
      title,
      description,
      rule_builder,
    });
  });

  it('should be able to convert Rule Builder to Source Code', () => {
    const title = 'title';
    const description = 'description';
    const rule_builder = { actions: [], conditions: [], operator: 'AND' };

    asMock(useRuleBuilder).mockReturnValue({
      rule: { title, description, rule_builder },
    } as any);

    const { getByRole } = renderWithDataRouter((
      <PipelineRulesContext.Provider value={{
        simulateRule: () => {},
        setRawMessageToSimulate: () => {},
        setRuleSimulationResult: () => {},
      }}>
        <RuleBuilder />
      </PipelineRulesContext.Provider>
    ));

    const convertButton = getByRole('button', { name: 'Convert Rule Builder to Source Code', hidden: true });
    userEvent.click(convertButton);

    const createRuleFromCodeButton = getByRole('button', { name: 'Create new Rule from Code', hidden: true });
    const copyCloseButton = getByRole('button', { name: 'Copy & Close', hidden: true });

    expect(createRuleFromCodeButton).toBeInTheDocument();
    expect(copyCloseButton).toBeInTheDocument();
  });

  it('should show simulator', () => {
    const title = 'title';
    const description = 'description';
    const rule_builder = { actions: [], conditions: [], operator: 'AND' };

    asMock(useRuleBuilder).mockReturnValue({
      rule: { title, description, rule_builder },
    } as any);

    const { getByText, getByRole } = renderWithDataRouter((
      <PipelineRulesContext.Provider value={{
        simulateRule: () => {},
        setRawMessageToSimulate: () => {},
        setRuleSimulationResult: () => {},
      }}>
        <RuleBuilder />
      </PipelineRulesContext.Provider>
    ));

    const ruleSimulationLabel = getByText('Rule Simulation');
    const runRuleSimulation = getByRole('button', { name: 'Run rule simulation' });

    expect(ruleSimulationLabel).toBeInTheDocument();
    expect(runRuleSimulation).toBeInTheDocument();
  });

  it('should show simulator with conditions and actions output', () => {
    const title = 'title';
    const description = 'description';
    const rule_builder = { actions: [], conditions: [], operator: 'AND' };
    const conditionOutput1 = 'condition_output_1';
    const actionOutput1 = 'action_output_1';
    const actionOutput2 = 'action_output_2';
    const rawMessageToSimulate = 'test';

    asMock(useRuleBuilder).mockReturnValue({
      rule: { title, description, rule_builder },
    } as any);

    const { getByText, getByTestId } = renderWithDataRouter((
      <PipelineRulesContext.Provider value={{
        ruleSimulationResult: {
          fields: { message: rawMessageToSimulate },
          simulator_condition_variables: { 1: conditionOutput1 },
          simulator_action_variables: [{ output_1: actionOutput1 }, { output_2: actionOutput2 }],
        },
        rawMessageToSimulate,
        simulateRule: () => {},
        setRawMessageToSimulate: () => {},
        setRuleSimulationResult: () => {},
      }}>
        <RuleBuilder />
      </PipelineRulesContext.Provider>
    ));

    const conditionOutputs = getByText('Conditions Output');
    const actionOutputs = getByText('Actions Output');
    const conditionOutputContainer = getByTestId('conditions-output');
    const actionOutputContainer = getByTestId('actions-output');

    expect(conditionOutputs).toBeInTheDocument();
    expect(actionOutputs).toBeInTheDocument();
    expect(conditionOutputContainer).toContainHTML(conditionOutput1);
    expect(actionOutputContainer).toContainHTML(actionOutput1);
    expect(actionOutputContainer).toContainHTML(actionOutput2);
  });

  it('simulator parser should support JSON', () => {
    const json = '{ "message": "test", "source": "unknown" }';

    expect(jsonifyText(json)).toEqual(json);
  });

  it('simulator parser should convert KeyValue pairs to JSON', () => {
    const keyValuePairs = 'a:a a a a\nb:b,b,b,b\n\n   f   :\n';
    const keyValuePairsAsJson = '{"a":"a a a a","b":"b,b,b,b"}';

    expect(jsonifyText(keyValuePairs)).toEqual(keyValuePairsAsJson);
  });

  it('simulator parser should support raw message string', () => {
    const rawMessageString = 'long raw message string bla bla bla';

    expect(jsonifyText(rawMessageString)).toEqual(rawMessageString);
  });
});
