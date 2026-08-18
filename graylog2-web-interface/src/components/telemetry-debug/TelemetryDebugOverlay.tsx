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
import { useState, useSyncExternalStore } from 'react';
import styled, { css } from 'styled-components';

import { Button, Table } from 'components/bootstrap';
import copyToClipboard from 'util/copyToClipboard';
import type { TelemetryDebugEntry } from 'logic/telemetry/debug/TelemetryDebugStore';
import { telemetryDebugStore, isTelemetryDebugEnabled } from 'logic/telemetry/debug/TelemetryDebugStore';

import TelemetryDebugEntryRow from './TelemetryDebugEntryRow';

const Badge = styled.button(
  ({ theme }) => css`
    position: fixed;
    right: ${theme.spacings.md};
    bottom: ${theme.spacings.md};
    z-index: 1050;
    border: 1px solid ${theme.colors.cards.border};
    border-radius: ${theme.spacings.xs};
    background: ${theme.colors.global.contentBackground};
    color: ${theme.colors.text.primary};
    padding: ${theme.spacings.xs} ${theme.spacings.sm};
    font-size: ${theme.fonts.size.small};
    box-shadow: 0 2px 8px rgb(0 0 0 / 20%);
  `,
);

const Drawer = styled.div(
  ({ theme }) => css`
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    height: 40vh;
    z-index: 1050;
    display: flex;
    flex-direction: column;
    background: ${theme.colors.global.contentBackground};
    border-top: 2px solid ${theme.colors.cards.border};
    box-shadow: 0 -2px 12px rgb(0 0 0 / 25%);
  `,
);

const Toolbar = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
    padding: ${theme.spacings.xs} ${theme.spacings.sm};
    border-bottom: 1px solid ${theme.colors.cards.border};
  `,
);

const Title = styled.strong(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    margin-right: ${theme.spacings.sm};
  `,
);

const FilterInput = styled.input(
  ({ theme }) => css`
    flex: 1;
    max-width: 300px;
    padding: 2px ${theme.spacings.xs};
    border: 1px solid ${theme.colors.cards.border};
    border-radius: ${theme.spacings.xs};
    background: ${theme.colors.global.contentBackground};
    color: ${theme.colors.text.primary};
  `,
);

const Body = styled.div(
  ({ theme }) => css`
    overflow-y: auto;
    padding: 0 ${theme.spacings.sm};
  `,
);

const EmptyHint = styled.p(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
    padding: ${theme.spacings.md};
    margin: 0;
  `,
);

const Spacer = styled.div`
  flex: 1;
`;

const matchesFilter = (entry: TelemetryDebugEntry, filter: string) => {
  if (!filter) return true;

  const needle = filter.toLowerCase();

  return (
    entry.eventType.toLowerCase().includes(needle) || JSON.stringify(entry.payload).toLowerCase().includes(needle)
  );
};

/**
 * Developer/QA overlay listing every telemetry event the app fires, with timing, payload, and
 * whether it was actually sent. Renders nothing unless the debug flag is on (see
 * TelemetryDebugStore); toggled via the dev-mode quick-jump action or localStorage.
 */
const TelemetryDebugOverlay = () => {
  const enabled = useSyncExternalStore(telemetryDebugStore.subscribe, isTelemetryDebugEnabled);
  const liveEntries = useSyncExternalStore(telemetryDebugStore.subscribe, telemetryDebugStore.getEntries);
  const [open, setOpen] = useState(false);
  const [frozenEntries, setFrozenEntries] = useState<TelemetryDebugEntry[] | null>(null);
  const [filter, setFilter] = useState('');
  const [expandedId, setExpandedId] = useState<number | null>(null);

  if (!enabled) return null;

  if (!open) {
    return (
      <Badge type="button" aria-label="Telemetry debug" onClick={() => setOpen(true)}>
        Telemetry debug: {liveEntries.length}
      </Badge>
    );
  }

  const entries = frozenEntries ?? liveEntries;
  // Newest first; deltas still relate each event to the one fired before it.
  const visible = entries.filter((entry) => matchesFilter(entry, filter)).reverse();
  const deltaFor = (entry: TelemetryDebugEntry) => {
    const index = entries.findIndex(({ id }) => id === entry.id);

    return index > 0 ? entry.timestamp - entries[index - 1].timestamp : null;
  };

  return (
    <Drawer>
      <Toolbar>
        <Title>Telemetry Debug ({entries.length})</Title>
        <FilterInput
          type="text"
          aria-label="Filter events"
          placeholder="Filter by event name or payload..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
        {frozenEntries ? (
          <Button bsSize="xsmall" onClick={() => setFrozenEntries(null)}>
            Resume
          </Button>
        ) : (
          <Button bsSize="xsmall" onClick={() => setFrozenEntries(liveEntries)}>
            Pause
          </Button>
        )}
        <Button bsSize="xsmall" onClick={() => telemetryDebugStore.clear()}>
          Clear
        </Button>
        <Button bsSize="xsmall" onClick={() => copyToClipboard(JSON.stringify(entries, null, 2))}>
          Export session
        </Button>
        <Spacer />
        <Button bsSize="xsmall" onClick={() => setOpen(false)}>
          Collapse
        </Button>
      </Toolbar>
      <Body>
        {visible.length === 0 ? (
          <EmptyHint>No telemetry events recorded yet — interact with the app to see them here.</EmptyHint>
        ) : (
          <Table condensed>
            <thead>
              <tr>
                <th>Time</th>
                <th>Event</th>
                <th>Action</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((entry) => (
                <TelemetryDebugEntryRow
                  key={entry.id}
                  entry={entry}
                  deltaMs={deltaFor(entry)}
                  expanded={expandedId === entry.id}
                  onToggle={() => setExpandedId(expandedId === entry.id ? null : entry.id)}
                />
              ))}
            </tbody>
          </Table>
        )}
      </Body>
    </Drawer>
  );
};

export default TelemetryDebugOverlay;
