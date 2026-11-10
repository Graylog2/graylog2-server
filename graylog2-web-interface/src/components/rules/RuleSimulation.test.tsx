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
import { render, screen } from 'wrappedTestingLibrary';

import RuleSimulation from 'components/rules/RuleSimulation';
import { PipelineRulesContext } from 'components/rules/RuleContext';

jest.mock('components/rules/rule-builder/RuleBuilderContext', () => ({
  useRuleBuilder: () => ({ useHighlightedOutput: [undefined, () => {}] }),
}));

// Integers outside of the safe range are parsed as BigInt from API responses.
const bigInteger = BigInt('12345678901234567890');

const rule = { id: 'rule-id', title: 'My rule', rule_builder: { conditions: [], actions: [] } };

const ruleSimulationResult = {
  fields: { message: 'Some message' },
  simulator_condition_variables: { 1: bigInteger },
  simulator_action_variables: [{ 1: bigInteger }],
};

const contextValue = {
  rule,
  simulateRule: () => {},
  rawMessageToSimulate: 'message: test',
  setRawMessageToSimulate: () => {},
  ruleSimulationResult,
  setRuleSimulationResult: () => {},
};

describe('RuleSimulation', () => {
  it('renders simulation output containing integers which exceed the safe integer range', async () => {
    render(
      <PipelineRulesContext.Provider value={contextValue}>
        <RuleSimulation rule={rule as any} />
      </PipelineRulesContext.Provider>,
    );

    const conditionsOutput = await screen.findByTestId('conditions-output');

    expect(conditionsOutput).toHaveTextContent('12345678901234567890');

    const actionsOutput = await screen.findByTestId('actions-output');

    expect(actionsOutput).toHaveTextContent('12345678901234567890');
  });
});
