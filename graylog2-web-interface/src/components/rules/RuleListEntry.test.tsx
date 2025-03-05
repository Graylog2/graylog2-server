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
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import type { ScopeParams } from 'hooks/useScopePermissions';
import useGetPermissionsByScope from 'hooks/useScopePermissions';

import RuleListEntry from './RuleListEntry';

jest.mock('hooks/useScopePermissions');

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
const SUT = ({ rule } : { rule: RuleType }) => (
  <table>
    <tbody>
      <RuleListEntry rule={rule} onDelete={onDeleteMock} usingPipelines={[]} />
    </tbody>
  </table>
);

describe('Rule', () => {
  let oldConfirm;

  beforeEach(() => {
    oldConfirm = window.confirm;
    window.confirm = jest.fn(() => true);
  })

  afterEach(() => {
    jest.clearAllMocks();
    window.confirm = oldConfirm
  });

  it('should render ruleListItem', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue({
      loadingScopePermissions: false,
      scopePermissions: undefined,
      checkPermissions: jest.fn()
    });
    render(<SUT rule={ruleMock} />);

    expect(await screen.findByRole('link', {
      name: /title1/i
    })).toBeInTheDocument();

    expect(screen.queryByText(/managed by application/i)).not.toBeInTheDocument();

    const deleteButton = screen.getByRole('button', {
      name: /delete rule/i
    });

    userEvent.click(deleteButton);

    expect(onDeleteMock).toHaveBeenCalledWith(ruleMock);
  });

  it('should render ruleListItem with managed by application', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue({
      loadingScopePermissions: false,
      scopePermissions: entityScope,
      checkPermissions: jest.fn()
    });
    render(<SUT rule={ruleMock} />);

    expect(await screen.findByRole('link', {
      name: /title1/i
    })).toBeInTheDocument();

    expect(await screen.findByText(/managed by application/i)).toBeInTheDocument();
  });
});
