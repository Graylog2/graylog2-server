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
import {
  telemetryDebugStore,
  isTelemetryDebugEnabled,
  setTelemetryDebugEnabled,
  TELEMETRY_DEBUG_STORAGE_KEY,
  MAX_ENTRIES,
} from './TelemetryDebugStore';

describe('TelemetryDebugStore', () => {
  beforeEach(() => {
    localStorage.clear();
    telemetryDebugStore.clear();
    setTelemetryDebugEnabled(true);
  });

  describe('enablement flag', () => {
    it('persists to localStorage and reads back', () => {
      setTelemetryDebugEnabled(false);

      expect(localStorage.getItem(TELEMETRY_DEBUG_STORAGE_KEY)).toBe('false');
      expect(isTelemetryDebugEnabled()).toBe(false);

      setTelemetryDebugEnabled(true);

      expect(localStorage.getItem(TELEMETRY_DEBUG_STORAGE_KEY)).toBe('true');
      expect(isTelemetryDebugEnabled()).toBe(true);
    });

    it('defaults to disabled without a stored flag', () => {
      localStorage.removeItem(TELEMETRY_DEBUG_STORAGE_KEY);
      setTelemetryDebugEnabled(false);
      localStorage.removeItem(TELEMETRY_DEBUG_STORAGE_KEY);

      expect(isTelemetryDebugEnabled()).toBe(false);
    });
  });

  describe('record', () => {
    it('appends an entry with type, payload, status, id, and timestamp', () => {
      telemetryDebugStore.record('Fleet Created', { fleet_id: 'f1' }, 'sent');

      const entries = telemetryDebugStore.getEntries();

      expect(entries).toHaveLength(1);
      expect(entries[0]).toEqual(
        expect.objectContaining({
          eventType: 'Fleet Created',
          payload: { fleet_id: 'f1' },
          status: 'sent',
        }),
      );
      expect(typeof entries[0].id).toBe('number');
      expect(typeof entries[0].timestamp).toBe('number');
    });

    it('assigns increasing ids', () => {
      telemetryDebugStore.record('A', {}, 'sent');
      telemetryDebugStore.record('B', {}, 'suppressed');

      const [first, second] = telemetryDebugStore.getEntries();

      expect(second.id).toBeGreaterThan(first.id);
    });

    it('does not record while debugging is disabled', () => {
      setTelemetryDebugEnabled(false);

      telemetryDebugStore.record('A', {}, 'sent');

      expect(telemetryDebugStore.getEntries()).toHaveLength(0);
    });

    it('drops the oldest entries beyond the cap', () => {
      for (let i = 0; i < MAX_ENTRIES + 1; i += 1) {
        telemetryDebugStore.record(`event-${i}`, {}, 'sent');
      }

      const entries = telemetryDebugStore.getEntries();

      expect(entries).toHaveLength(MAX_ENTRIES);
      expect(entries[0].eventType).toBe('event-1');
      expect(entries[entries.length - 1].eventType).toBe(`event-${MAX_ENTRIES}`);
    });

    it('returns a stable snapshot reference between mutations', () => {
      telemetryDebugStore.record('A', {}, 'sent');

      const first = telemetryDebugStore.getEntries();
      const second = telemetryDebugStore.getEntries();

      expect(first).toBe(second);

      telemetryDebugStore.record('B', {}, 'sent');

      expect(telemetryDebugStore.getEntries()).not.toBe(first);
    });
  });

  describe('clear', () => {
    it('empties the buffer', () => {
      telemetryDebugStore.record('A', {}, 'sent');

      telemetryDebugStore.clear();

      expect(telemetryDebugStore.getEntries()).toHaveLength(0);
    });
  });

  describe('module duplication', () => {
    // Plugin bundles compile their own copies of core modules (which is why TelemetryContext,
    // useSendTelemetry, and App are singleton()-wrapped). A provider from one bundle must land
    // its events in the same store an overlay from another bundle reads.
    it('shares state across duplicate module copies', () => {
      telemetryDebugStore.record('From First Copy', {}, 'sent');

      let secondCopy: {
        telemetryDebugStore: typeof telemetryDebugStore;
        isTelemetryDebugEnabled: typeof isTelemetryDebugEnabled;
      };
      jest.isolateModules(() => {
        secondCopy = jest.requireActual('./TelemetryDebugStore');
      });

      expect(secondCopy.telemetryDebugStore.getEntries()).toHaveLength(1);
      expect(secondCopy.telemetryDebugStore.getEntries()[0].eventType).toBe('From First Copy');
      expect(secondCopy.isTelemetryDebugEnabled()).toBe(true);
    });
  });

  describe('subscribe', () => {
    it('notifies on record, clear, and flag changes until unsubscribed', () => {
      const listener = jest.fn();
      const unsubscribe = telemetryDebugStore.subscribe(listener);

      telemetryDebugStore.record('A', {}, 'sent');
      telemetryDebugStore.clear();
      setTelemetryDebugEnabled(false);

      expect(listener).toHaveBeenCalledTimes(3);

      unsubscribe();
      setTelemetryDebugEnabled(true);
      telemetryDebugStore.record('B', {}, 'sent');

      expect(listener).toHaveBeenCalledTimes(3);
    });
  });
});
