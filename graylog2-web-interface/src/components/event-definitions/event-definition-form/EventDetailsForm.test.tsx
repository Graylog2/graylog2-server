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
import React, { useState } from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { simpleEventDefinition } from 'fixtures/eventDefinition';
import { asMock } from 'helpers/mocking';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import usePluginEntities from 'hooks/usePluginEntities';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { SYSTEM_EVENT_DEFINITION_TYPE } from 'components/event-definitions/constants';

import EventDetailsForm from './EventDetailsForm';

jest.mock('hooks/usePluggableLicenseCheck');
jest.mock('hooks/usePluginEntities');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('./EventSummaryTemplateHelp', () => () => null);
jest.mock('components/common', () => ({
  Select: jest.requireActual('components/common/Select').default,
}));

const licenseCheck = (valid: boolean): ReturnType<typeof usePluggableLicenseCheck> => ({
  data: { valid, expired: false, violated: false },
  isInitialLoading: false,
  refetch: () => {},
});

const defaultProps = {
  eventDefinition: { ...simpleEventDefinition, remediation_steps: 'Restart the service.' },
  eventDefinitionEventProcedure: '',
  validation: { errors: {} },
  onChange: jest.fn(),
  canEdit: true,
};

const EditableEventDetailsForm = ({ procedureId }: { procedureId: string }) => {
  const [eventDefinition, setEventDefinition] = useState(defaultProps.eventDefinition);

  return (
    <EventDetailsForm
      {...defaultProps}
      eventDefinition={eventDefinition}
      eventDefinitionEventProcedure={procedureId}
      onChange={(name, value) => {
        defaultProps.onChange(name, value);
        setEventDefinition((current) => ({ ...current, [name]: value }));
      }}
    />
  );
};

const EventProcedureSummary = ({ eventProcedureId, onEdit }: { eventProcedureId: string; onEdit: () => void }) => (
  <>
    <p>Procedure: {eventProcedureId}</p>
    <button type="button" onClick={onEdit}>
      Edit Event Procedure
    </button>
  </>
);

const EventProcedureForm = ({ remediationSteps }: { remediationSteps: string }) => (
  <p>Event Procedure Editor: {remediationSteps}</p>
);

describe('EventDetailsForm', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(false));
    asMock(usePluginEntities).mockImplementation((key) => {
      if (key === 'views.components.eventProcedureSummary') {
        return [{ component: EventProcedureSummary }];
      }

      if (key === 'views.components.eventProcedureForm') {
        return [{ component: EventProcedureForm }];
      }

      return [];
    });
    asMock(useSendTelemetry).mockReturnValue(jest.fn());
  });

  it.each(['', 'procedure-id'])(
    'allows unlicensed users to edit remediation steps with procedure "%s"',
    async (procedureId) => {
      render(<EditableEventDetailsForm procedureId={procedureId} />);

      const input = screen.getByRole('textbox', { name: 'Remediation Steps' });

      expect(input).toHaveValue('Restart the service.');
      expect(input).not.toHaveAttribute('readonly');
      expect(screen.queryByText('Event Procedure Summary')).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Add Event Procedure' })).not.toBeInTheDocument();
      expect(screen.queryByText(`Procedure: ${procedureId}`)).not.toBeInTheDocument();

      await userEvent.clear(input);
      await userEvent.type(input, '**Restart** the service.\nCheck the logs.');

      expect(input).toHaveValue('**Restart** the service.\nCheck the logs.');
      expect(defaultProps.onChange).toHaveBeenLastCalledWith(
        'remediation_steps',
        '**Restart** the service.\nCheck the logs.',
      );

      await userEvent.clear(input);

      expect(input).toHaveValue('');
      expect(defaultProps.onChange).toHaveBeenLastCalledWith('remediation_steps', '');
    },
  );

  it('keeps remediation steps read-only without edit permission', async () => {
    render(<EventDetailsForm {...defaultProps} canEdit={false} />);

    const input = screen.getByRole('textbox', { name: 'Remediation Steps' });

    expect(input).toHaveAttribute('readonly');

    await userEvent.type(input, 'Changed');

    expect(defaultProps.onChange).not.toHaveBeenCalled();
  });

  it('keeps remediation steps read-only for system event definitions', () => {
    render(
      <EventDetailsForm
        {...defaultProps}
        eventDefinition={{
          ...defaultProps.eventDefinition,
          config: { ...simpleEventDefinition.config, type: SYSTEM_EVENT_DEFINITION_TYPE },
        }}
      />,
    );

    expect(screen.getByRole('textbox', { name: 'Remediation Steps' })).toHaveAttribute('readonly');
  });

  it('allows licensed users to add an event procedure instead of editing remediation steps', async () => {
    asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(true));
    render(<EventDetailsForm {...defaultProps} />);

    expect(screen.queryByRole('textbox', { name: 'Remediation Steps' })).not.toBeInTheDocument();
    expect(screen.getByText('Event Procedure Summary')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Add Event Procedure' }));

    await screen.findByText('Event Procedure Editor: Restart the service.');
  });

  it('allows licensed users to view and edit an existing event procedure', async () => {
    asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(true));
    render(<EventDetailsForm {...defaultProps} eventDefinitionEventProcedure="procedure-id" />);

    expect(screen.getByText('Event Procedure Summary')).toBeInTheDocument();
    expect(screen.getByText('Procedure: procedure-id')).toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: 'Remediation Steps' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add Event Procedure' })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Edit Event Procedure' }));

    await screen.findByText('Event Procedure Editor: Restart the service.');
  });

  it('falls back to remediation steps if the license becomes invalid while the procedure editor is open', async () => {
    asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(true));
    const { rerender } = render(<EventDetailsForm {...defaultProps} />);

    await userEvent.click(screen.getByRole('button', { name: 'Add Event Procedure' }));

    await screen.findByText('Event Procedure Editor: Restart the service.');

    asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(false));
    rerender(<EventDetailsForm {...defaultProps} />);

    expect(screen.getByRole('textbox', { name: 'Remediation Steps' })).toHaveValue('Restart the service.');
    expect(screen.queryByText('Event Procedure Editor: Restart the service.')).not.toBeInTheDocument();
  });
});
