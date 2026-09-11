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
import styled, { css } from 'styled-components';

import { Button, Label } from 'components/bootstrap';
import copyToClipboard from 'util/copyToClipboard';
import type { TelemetryDebugEntry, TelemetryDebugStatus } from 'logic/telemetry/debug/TelemetryDebugStore';
import detectPiiSuspect from 'logic/telemetry/debug/piiHeuristics';

type Props = {
  entry: TelemetryDebugEntry;
  /** Milliseconds since the previous (older) event, or null for the oldest one on record. */
  deltaMs: number | null;
  expanded: boolean;
  onToggle: () => void;
};

const ClickableCell = styled.td`
  cursor: pointer;
`;

const TimeCell = styled.td(
  ({ theme }) => css`
    font-family: ${theme.fonts.family.monospace};
    font-size: ${theme.fonts.size.small};
    white-space: nowrap;
  `,
);

const PayloadBlock = styled.div(
  ({ theme }) => css`
    font-family: ${theme.fonts.family.monospace};
    font-size: ${theme.fonts.size.small};
    background: ${theme.colors.global.contentBackground};
    border: 1px solid ${theme.colors.cards.border};
    border-radius: ${theme.spacings.xs};
    padding: ${theme.spacings.sm};
    margin: ${theme.spacings.xs} 0;
    white-space: pre-wrap;
    word-break: break-all;
  `,
);

const SuspectValue = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.variant.danger};
    font-weight: bold;
  `,
);

const STATUS_STYLE = {
  sent: 'success',
  suppressed: 'warning',
  disabled: 'default',
} as const satisfies Record<TelemetryDebugStatus, string>;

const formatTime = (timestamp: number) => new Date(timestamp).toISOString().slice(11, 23);

const PayloadLine = ({ property, value }: { property: string; value: unknown }) => {
  const suspect = detectPiiSuspect(value);
  const rendered = JSON.stringify(value);

  return (
    <div>
      &quot;{property}&quot;: {suspect ? <SuspectValue title={`PII suspect: ${suspect}`}>{rendered}</SuspectValue> : rendered}
    </div>
  );
};

const TelemetryDebugEntryRow = ({ entry, deltaMs, expanded, onToggle }: Props) => {
  const appActionValue = (entry.payload as { app_action_value?: string }).app_action_value;

  return (
    <>
      <tr>
        <TimeCell>
          {formatTime(entry.timestamp)}
          {deltaMs !== null && ` (+${deltaMs}ms)`}
        </TimeCell>
        <ClickableCell onClick={onToggle}>{entry.eventType}</ClickableCell>
        <td>{appActionValue}</td>
        <td>
          <Label bsStyle={STATUS_STYLE[entry.status]} bsSize="xs">
            {entry.status}
          </Label>
        </td>
      </tr>
      {expanded && (
        <tr>
          <td colSpan={4}>
            <PayloadBlock>
              {Object.entries(entry.payload).map(([property, value]) => (
                <PayloadLine key={property} property={property} value={value} />
              ))}
            </PayloadBlock>
            <Button
              bsSize="xsmall"
              onClick={() =>
                copyToClipboard(
                  JSON.stringify(
                    { eventType: entry.eventType, timestamp: entry.timestamp, status: entry.status, ...entry.payload },
                    null,
                    2,
                  ),
                )
              }>
              Copy event as JSON
            </Button>
          </td>
        </tr>
      )}
    </>
  );
};

export default TelemetryDebugEntryRow;
