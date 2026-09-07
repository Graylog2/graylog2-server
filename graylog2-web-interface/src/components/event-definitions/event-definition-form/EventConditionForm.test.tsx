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
import { defaultUser as mockDefaultUser } from 'defaultMockValues';

import { asMock } from 'helpers/mocking';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import usePluginEntities from 'hooks/usePluginEntities';
import { simpleEventDefinition as mockEventDefinition } from 'fixtures/eventDefinition';

import EventConditionForm from './EventConditionForm';

jest.mock('routing/useLocation');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('hooks/usePluginEntities');

const StubFormComponent = () => <div>Stub Condition Form</div>;

// Two distinct non-creatable types, so the assertions prove the guard keys off
// `hideFromCreation` rather than off any particular type name. Plugin entities are mocked, so
// these names are fixtures and need not match any registered condition type.
const hiddenType1 = {
  type: 'hidden-type-v1',
  displayName: 'Hidden Type 1',
  useCondition: () => true,
  hideFromCreation: true,
  formComponent: StubFormComponent,
};

const hiddenType2 = {
  type: 'hidden-type-v2',
  displayName: 'Hidden Type 2',
  useCondition: () => true,
  hideFromCreation: true,
  formComponent: StubFormComponent,
};

const aggregationType = {
  type: 'aggregation-v1',
  displayName: 'Filter & Aggregation',
  useCondition: () => true,
  formComponent: StubFormComponent,
};

const mockValidation = { errors: {} };

const renderConditionForm = (
  props: Partial<React.ComponentProps<typeof EventConditionForm>> = {},
  eventDefinitionTypes: Array<any> = [aggregationType],
) => {
  asMock(usePluginEntities).mockImplementation((entityKey) =>
    entityKey === 'eventDefinitionTypes' ? eventDefinitionTypes : [],
  );

  return render(
    <EventConditionForm
      action="create"
      eventDefinition={mockEventDefinition}
      validation={mockValidation}
      currentUser={mockDefaultUser}
      onChange={jest.fn()}
      canEdit
      {...props}
    />,
  );
};

describe('EventConditionForm', () => {
  beforeEach(() => {
    asMock(useLocation).mockImplementation(() => ({
      pathname: '/event-definitions',
      search: '',
      hash: '',
      state: null,
      key: 'mock-key',
    }));
    asMock(useSendTelemetry).mockImplementation(() => jest.fn());
  });

  it('disables the select when editing a hideFromCreation type', async () => {
    renderConditionForm(
      {
        action: 'edit',
        eventDefinition: {
          ...mockEventDefinition,
          config: { ...mockEventDefinition.config, type: 'hidden-type-v2' },
        },
      },
      [hiddenType2, aggregationType],
    );

    expect(await screen.findByLabelText('Condition Type')).toBeDisabled();
  });

  it('disables the select for a second, differently named hideFromCreation type', async () => {
    renderConditionForm(
      {
        action: 'edit',
        eventDefinition: {
          ...mockEventDefinition,
          config: { ...mockEventDefinition.config, type: 'hidden-type-v1' },
        },
      },
      [hiddenType1, aggregationType],
    );

    expect(await screen.findByLabelText('Condition Type')).toBeDisabled();
  });

  it('leaves the select enabled when editing a normal creatable type', async () => {
    renderConditionForm(
      {
        action: 'edit',
        eventDefinition: {
          ...mockEventDefinition,
          config: { ...mockEventDefinition.config, type: 'aggregation-v1' },
        },
      },
      [aggregationType],
    );

    expect(await screen.findByLabelText('Condition Type')).toBeEnabled();
  });
});
