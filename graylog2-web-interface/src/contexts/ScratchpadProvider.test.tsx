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
import { useContext } from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import HotkeysProvider from 'contexts/HotkeysProvider';
import Store from 'logic/local-storage/Store';

import { ScratchpadContext, ScratchpadProvider } from './ScratchpadProvider';

jest.mock('logic/telemetry/useSendTelemetry');

const LOCAL_STORAGE_ITEM = 'gl-scratchpad-someone';

const Consumer = () => {
  const { isScratchpadVisible, setScratchpadVisibility, toggleScratchpadVisibility } = useContext(ScratchpadContext);

  return (
    <>
      <span>{isScratchpadVisible ? 'visible' : 'hidden'}</span>
      <button type="button" onClick={toggleScratchpadVisibility}>
        Toggle
      </button>
      <button type="button" onClick={() => setScratchpadVisibility(true)}>
        Open
      </button>
      <button type="button" onClick={() => setScratchpadVisibility(false)}>
        Close
      </button>
    </>
  );
};

const SUT = () => (
  <HotkeysProvider>
    <ScratchpadProvider loginName="someone">
      <Consumer />
    </ScratchpadProvider>
  </HotkeysProvider>
);

const scratchpadEvents = (mock: jest.Mock) =>
  mock.mock.calls
    .map(([eventType]) => eventType)
    .filter((eventType) => Object.values(TELEMETRY_EVENT_TYPE.SCRATCHPAD).includes(eventType));

describe('ScratchpadProvider', () => {
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    Store.delete(LOCAL_STORAGE_ITEM);
    sendTelemetry.mockClear();
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
  });

  it('uses the scratchpad app section for telemetry', async () => {
    render(<SUT />);

    await screen.findByText('hidden');

    expect(useSendTelemetry).toHaveBeenCalledWith('scratchpad');
  });

  it('sends opened and closed events when toggling', async () => {
    render(<SUT />);

    await userEvent.click(await screen.findByRole('button', { name: 'Toggle' }));
    await screen.findByText('visible');

    expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.SCRATCHPAD.OPENED, {});

    await userEvent.click(screen.getByRole('button', { name: 'Toggle' }));
    await screen.findByText('hidden');

    expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.SCRATCHPAD.CLOSED, {});
    expect(scratchpadEvents(sendTelemetry)).toEqual([
      TELEMETRY_EVENT_TYPE.SCRATCHPAD.OPENED,
      TELEMETRY_EVENT_TYPE.SCRATCHPAD.CLOSED,
    ]);
  });

  it('sends opened and closed events when setting the visibility', async () => {
    render(<SUT />);

    await userEvent.click(await screen.findByRole('button', { name: 'Open' }));
    await screen.findByText('visible');
    await userEvent.click(screen.getByRole('button', { name: 'Close' }));
    await screen.findByText('hidden');

    expect(scratchpadEvents(sendTelemetry)).toEqual([
      TELEMETRY_EVENT_TYPE.SCRATCHPAD.OPENED,
      TELEMETRY_EVENT_TYPE.SCRATCHPAD.CLOSED,
    ]);
  });

  it('does not send events when the visibility does not change', async () => {
    render(<SUT />);

    await userEvent.click(await screen.findByRole('button', { name: 'Close' }));
    await userEvent.click(screen.getByRole('button', { name: 'Open' }));
    await screen.findByText('visible');
    await userEvent.click(screen.getByRole('button', { name: 'Open' }));

    expect(scratchpadEvents(sendTelemetry)).toEqual([TELEMETRY_EVENT_TYPE.SCRATCHPAD.OPENED]);
  });

  it('does not send events for the initial visibility restored from local storage', async () => {
    Store.set(LOCAL_STORAGE_ITEM, { opened: true });

    render(<SUT />);

    await screen.findByText('visible');

    expect(scratchpadEvents(sendTelemetry)).toEqual([]);
  });

  it('sends an opened event when opening with the keyboard shortcut', async () => {
    render(<SUT />);

    await screen.findByText('hidden');
    await userEvent.keyboard('{Control>}/{/Control}');
    await screen.findByText('visible');

    expect(scratchpadEvents(sendTelemetry)).toEqual([TELEMETRY_EVENT_TYPE.SCRATCHPAD.OPENED]);
    expect(Store.get(LOCAL_STORAGE_ITEM)).toEqual(expect.objectContaining({ opened: true }));
  });

  it('does not send an opened event for the keyboard shortcut when already open', async () => {
    Store.set(LOCAL_STORAGE_ITEM, { opened: true });

    render(<SUT />);

    await screen.findByText('visible');
    await userEvent.keyboard('{Control>}/{/Control}');

    expect(scratchpadEvents(sendTelemetry)).toEqual([]);
  });
});
