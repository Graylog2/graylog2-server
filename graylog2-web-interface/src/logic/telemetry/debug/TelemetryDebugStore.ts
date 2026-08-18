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
import { singleton } from 'logic/singleton';

// 'sent' = posthog.capture ran; 'suppressed' = posthog gates dropped it; 'disabled' = telemetry
// is turned off entirely and the event only exists for debugging.
export type TelemetryDebugStatus = 'sent' | 'suppressed' | 'disabled';

export type TelemetryDebugEntry = {
  id: number;
  timestamp: number;
  eventType: string;
  payload: object;
  status: TelemetryDebugStatus;
};

export const TELEMETRY_DEBUG_STORAGE_KEY = 'gl.telemetry-debug';
export const MAX_ENTRIES = 500;

const createTelemetryDebugStore = () => {
  // The flag is mirrored into memory so the per-event hot path is a boolean check, not a
  // localStorage read.
  let enabled = false;
  let entries: TelemetryDebugEntry[] = [];
  let nextId = 1;
  const listeners = new Set<() => void>();

  try {
    enabled = localStorage.getItem(TELEMETRY_DEBUG_STORAGE_KEY) === 'true';
  } catch {
    // localStorage unavailable (e.g. privacy mode) — debugging simply stays off.
  }

  const notify = () => listeners.forEach((listener) => listener());

  return {
    isTelemetryDebugEnabled: () => enabled,
    setTelemetryDebugEnabled: (newEnabled: boolean) => {
      enabled = newEnabled;

      try {
        localStorage.setItem(TELEMETRY_DEBUG_STORAGE_KEY, String(newEnabled));
      } catch {
        // Persistence is best-effort; the in-memory flag still applies for this session.
      }

      notify();
    },
    telemetryDebugStore: {
      record: (eventType: string, payload: object, status: TelemetryDebugStatus) => {
        if (!enabled) return;

        entries = [...entries, { id: nextId, timestamp: Date.now(), eventType, payload, status }].slice(-MAX_ENTRIES);
        nextId += 1;

        notify();
      },
      getEntries: () => entries,
      clear: () => {
        entries = [];

        notify();
      },
      subscribe: (listener: () => void) => {
        listeners.add(listener);

        return () => listeners.delete(listener);
      },
    },
  };
};

// Plugin bundles compile their own copies of core modules, so the store state must live in the
// cross-bundle singleton registry — a provider in one bundle records into the same buffer an
// overlay from another bundle reads (same reason TelemetryContext and App are singletons).
export const { isTelemetryDebugEnabled, setTelemetryDebugEnabled, telemetryDebugStore } = singleton(
  'core.TelemetryDebugStore',
  createTelemetryDebugStore,
);
