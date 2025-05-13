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
import type { RuleType } from 'src/stores/rules/RulesStore';

import { asMock, StoreMock as MockStore } from 'helpers/mocking';
import DefaultQueryClientProvider from 'contexts/DefaultQueryClientProvider';
import mockComponent from 'helpers/mocking/MockComponent';
import type { ScopeParams } from 'hooks/useScopePermissions';
import useGetPermissionsByScope from 'hooks/useScopePermissions';

import Rule from './Rule';
import { PipelineRulesContext } from './RuleContext';

jest.mock('stores/rules/RulesStore', () => ({ RulesStore: MockStore() }));
jest.mock('./rule-helper/RuleHelper', () => mockComponent('RuleHelper'));
jest.mock('./RuleForm', () => mockComponent('RuleForm'));
jest.mock('./rule-builder/RuleBuilder', () => mockComponent('RuleBuilder'));
jest.mock('hooks/useScopePermissions');

const logger = {
  // eslint-disable-next-line no-console
  log: console.log,
  // eslint-disable-next-line no-console
  warn: console.warn,
  error: () => {},
};

const ruleMock = {
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
  rule_builder: false,
} as unknown as RuleType;
const handleDescription = jest.fn();
const handleSavePipelineRule = jest.fn();
const entityScope = {
  is_mutable: false,
} as unknown as ScopeParams;

const SUT = ({ rule }: { rule: RuleType }) => (
  <DefaultQueryClientProvider options={{ logger }}>
    <PipelineRulesContext.Provider
      value={{
        rule,
        description: '',
        handleDescription: handleDescription,
        ruleSource: rule.source,
        handleSavePipelineRule,
        ruleSourceRef: {},
        usedInPipelines: [],
        onAceLoaded: () => {},
        onChangeSource: () => {},
        setRawMessageToSimulate: () => {},
        setRuleSimulationResult: () => {},
      }}>
      <Rule create={false} title={rule.title} />
    </PipelineRulesContext.Provider>
  </DefaultQueryClientProvider>
);

describe('Rule', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should not render managed alert for unscoped rule', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue({
      loadingScopePermissions: false,
      scopePermissions: undefined,
      checkPermissions: jest.fn(),
    });
    render(<SUT rule={ruleMock} />);
    await screen.findByText('title1');

    expect(screen.queryByText('this rule is managed by application')).not.toBeInTheDocument();
  });

  it('should render managed alert for scoped rule', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue({
      loadingScopePermissions: false,
      scopePermissions: entityScope,
      checkPermissions: jest.fn(),
    });
    render(<SUT rule={{ ...ruleMock, _scope: 'GRAYLOG_SYSTEM_PIPELINE_RULE_SCOPE' }} />);
    await screen.findByText('title1');

    expect(await screen.findByText(/this rule is managed by application/i)).toBeInTheDocument();
  });
});
