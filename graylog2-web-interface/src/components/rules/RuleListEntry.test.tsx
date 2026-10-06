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
import userEvent from '@testing-library/user-event';

import type { RuleType } from 'components/rules/hooks/useRules';
import { asMock } from 'helpers/mocking';
import type { ScopeParams } from 'hooks/useScopePermissions';
import useGetPermissionsByScope from 'hooks/useScopePermissions';
import type { ProcessingLoadResponse } from 'components/pipelines/processing-load';

import RuleListEntry from './RuleListEntry';

jest.mock('hooks/useScopePermissions');

const baseProcessingLoad: ProcessingLoadResponse = {
  available: true,
  total_cost_microseconds_per_second: 100,
  pipelines: [],
  rules: [
    { rule_id: 'rule-1', load_percent: 42.4242 },
    { rule_id: 'rule-zero', load_percent: 0 },
  ],
  stage_rules: [],
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
  created_at: '2025-02-20T11:56:40.011Z',
  modified_at: '2025-02-20T11:56:40.011Z',
  rule_builder: false,
} as unknown as RuleType;
const onDeleteMock = jest.fn();
const entityScope = {
  is_mutable: false,
} as unknown as ScopeParams;
type SUTProps = { rule: RuleType } & Partial<Omit<React.ComponentProps<typeof RuleListEntry>, 'rule'>>;

const SUT = ({ rule, ...props }: SUTProps) => (
  <table>
    <tbody>
      <RuleListEntry rule={rule} onDelete={onDeleteMock} usingPipelines={[]} {...props} />
    </tbody>
  </table>
);

describe('Rule', () => {
  let oldConfirm: typeof window.confirm;

  beforeEach(() => {
    oldConfirm = window.confirm;
    window.confirm = jest.fn(() => true);
  });

  afterEach(() => {
    jest.clearAllMocks();
    window.confirm = oldConfirm;
  });

  it('should render ruleListItem', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue({
      loadingScopePermissions: false,
      scopePermissions: undefined,
      checkPermissions: jest.fn(),
    });
    render(<SUT rule={ruleMock} />);

    expect(
      await screen.findByRole('link', {
        name: /title1/i,
      }),
    ).toBeInTheDocument();

    expect(screen.queryByText(/managed by application/i)).not.toBeInTheDocument();

    const deleteButton = screen.getByRole('button', {
      name: /delete rule/i,
    });

    await userEvent.click(deleteButton);

    expect(onDeleteMock).toHaveBeenCalledWith(ruleMock);
  });

  it('should render ruleListItem with managed by application', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue({
      loadingScopePermissions: false,
      scopePermissions: entityScope,
      checkPermissions: jest.fn(),
    });
    render(<SUT rule={ruleMock} />);

    expect(
      await screen.findByRole('link', {
        name: /title1/i,
      }),
    ).toBeInTheDocument();

    expect(await screen.findByText(/managed by application/i)).toBeInTheDocument();
  });
});

describe('RuleListEntry Pipeline Load cell', () => {
  beforeEach(() => {
    asMock(useGetPermissionsByScope).mockReturnValue({
      loadingScopePermissions: false,
      scopePermissions: undefined,
      checkPermissions: jest.fn(),
    });
  });

  it('does not render the load cell when showLoadColumn is false', () => {
    render(
      <SUT
        rule={{ ...ruleMock, id: 'rule-1' } as RuleType}
        showLoadColumn={false}
        processingLoad={baseProcessingLoad}
      />,
    );

    expect(screen.queryByText(/%$/)).not.toBeInTheDocument();
  });

  it('renders the load percent formatted to two decimals', () => {
    render(<SUT rule={{ ...ruleMock, id: 'rule-1' } as RuleType} showLoadColumn processingLoad={baseProcessingLoad} />);

    expect(screen.getByText('42.42%')).toBeInTheDocument();
  });

  it('renders 0.00% for participating zero-cost rules', () => {
    render(
      <SUT rule={{ ...ruleMock, id: 'rule-zero' } as RuleType} showLoadColumn processingLoad={baseProcessingLoad} />,
    );

    expect(screen.getByText('0.00%')).toBeInTheDocument();
  });

  it('renders blank for rules missing from the response', () => {
    render(
      <SUT rule={{ ...ruleMock, id: 'unknown-rule' } as RuleType} showLoadColumn processingLoad={baseProcessingLoad} />,
    );

    expect(screen.queryByText(/%$/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Pipeline Load is unavailable/i)).not.toBeInTheDocument();
  });

  it('renders blank when total_cost is zero (denominator-zero)', () => {
    render(
      <SUT
        rule={{ ...ruleMock, id: 'rule-1' } as RuleType}
        showLoadColumn
        processingLoad={{ ...baseProcessingLoad, total_cost_microseconds_per_second: 0 }}
      />,
    );

    expect(screen.queryByText(/%$/)).not.toBeInTheDocument();
  });

  it('renders an em-dash with an accessible error label when processingLoadError is true', () => {
    render(
      <SUT
        rule={{ ...ruleMock, id: 'rule-1' } as RuleType}
        showLoadColumn
        processingLoad={undefined}
        processingLoadError
      />,
    );

    expect(screen.getByLabelText(/Pipeline Load is unavailable/i)).toBeInTheDocument();
  });
});
