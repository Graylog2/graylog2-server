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
import { useState } from 'react';
import { act, renderWithDataRouter, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';

import RuleForm from './RuleForm';
import { PipelineRulesContext } from './RuleContext';

const extendedTimeout = applyTimeoutMultiplier(30000);

describe('RuleForm', () => {
  it('should save and update the correct description value', async () => {
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
    const handleSavePipelineRule = jest.fn();

    const { getByLabelText, getByRole } = renderWithDataRouter((
      <PipelineRulesContext.Provider value={{
        description: '',
        handleDescription: handleDescription,
        ruleSource: ruleToUpdate.source,
        handleSavePipelineRule,
        ruleSourceRef: {},
        usedInPipelines: [],
        onAceLoaded: () => {},
        onChangeSource: () => {},
        setRawMessageToSimulate: () => {},
        setRuleSimulationResult: () => {},
      }}>
        <RuleForm create={false} />
      </PipelineRulesContext.Provider>
    ));

    const descriptionInput = getByLabelText('Description');

    expect(descriptionInput).toHaveValue('');

    await userEvent.paste(descriptionInput, ruleToUpdate.description);
    const createRuleButton = getByRole('button', { name: 'Update rule & close' });

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.click(createRuleButton);
    });

    await waitFor(() => {
      expect(handleDescription).toHaveBeenCalledWith(ruleToUpdate.description);
    });
  });

  it('should run rule simulation using the rule input', async () => {
    const ruleToUpdate = {
      source: `rule "concat new_"
      when
      has_field("message")
      then
        set_field("message", concat("new_",to_string($message.message)));
      end`,
    };

    const _setRawMessage = jest.fn();
    const ruleInput = 'new_test';

    const PipelineRulesContextProvider = ({ children, setRawMessage }: React.PropsWithChildren<{ setRawMessage: (message: string) => void }>) => {
      const [rawMessageToSimulate, _setRawMessageToSimulate] = useState('');

      const setRawMessageToSimulate = (message: string) => {
        setRawMessage(message);
        _setRawMessageToSimulate(message);
      };

      return (
        <PipelineRulesContext.Provider value={{
          ruleSource: ruleToUpdate.source,
          ruleSourceRef: {},
          usedInPipelines: [],
          rawMessageToSimulate,
          setRawMessageToSimulate,
          setRuleSimulationResult: () => {},
          simulateRule: () => {},
        }}>
          {children}
        </PipelineRulesContext.Provider>
      );
    };

    renderWithDataRouter(
      <PipelineRulesContextProvider setRawMessage={_setRawMessage}>
        <RuleForm create={false} />
      </PipelineRulesContextProvider>,
    );

    const rawMessageInput = await screen.findByTitle('Message string or JSON');

    expect(rawMessageInput).toHaveValue('');

    await userEvent.paste(rawMessageInput, ruleInput);
    const runSimulationButton = await screen.findByRole('button', { name: 'Run rule simulation' });

    await waitFor(() => {
      expect(runSimulationButton).not.toBeDisabled();
    });

    await userEvent.click(runSimulationButton);

    await waitFor(() => {
      expect(_setRawMessage).toHaveBeenCalledWith(ruleInput);
    });
  }, extendedTimeout);
});
