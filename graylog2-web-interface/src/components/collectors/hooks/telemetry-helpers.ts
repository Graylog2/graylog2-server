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
export type HostnameKind = 'ip' | 'hostname';
export type InputBindType = 'wildcard' | 'specific' | 'unknown';
export type InputStateClassification = 'running' | 'stopped' | 'failed' | 'not_configured' | 'unknown';

// Structural types — avoid importing concrete Input / InputState types to keep
// this module decoupled from whatever shape those stores currently expose.
type InputLike = { attributes?: { port?: number; bind_address?: string } };
type InputIdLike = { id?: string };
type InputStateLike = { id: string; state: string };

const IPV4_PATTERN = /^(\d{1,3}\.){3}\d{1,3}$/;
const IPV6_PATTERN = /^[0-9a-fA-F:]+$/;

// Classifies a hostname string as an IP address or a DNS-style name.
// Empty string treated as 'hostname' (PostHog breakdown defaults are better than omitting).
export const classifyHostname = (value: string): HostnameKind => {
  if (!value) return 'hostname';
  if (IPV4_PATTERN.test(value)) return 'ip';
  if (value.includes(':') && IPV6_PATTERN.test(value)) return 'ip';

  return 'hostname';
};

// Classifies an Input's bind_address as wildcard (listens on any interface) or specific.
export const classifyInputBind = (bindAddress: string | undefined | null): InputBindType => {
  if (bindAddress === undefined || bindAddress === null) return 'unknown';
  if (bindAddress === '' || bindAddress === '0.0.0.0' || bindAddress === '::' || bindAddress === '*') {
    return 'wildcard';
  }

  return 'specific';
};

// Literal port equality; null when input or port cannot be read.
export const comparePort = (settingsPort: number, input: InputLike | undefined): boolean | null => {
  const inputPort = input?.attributes?.port;

  if (inputPort === undefined || inputPort === null) return null;

  return inputPort === settingsPort;
};

// Returns runtime state of the backing Input. 'not_configured' means no input is set at all.
// 'unknown' means we know there's an input but couldn't look up its state.
export const classifyInputState = (
  input: InputIdLike | undefined,
  inputStates: Array<InputStateLike> | undefined,
): InputStateClassification => {
  if (!input) return 'not_configured';
  if (!inputStates) return 'unknown';

  const match = inputStates.find((s) => s.id === input.id);

  if (!match) return 'unknown';

  const normalised = String(match.state).toLowerCase();

  if (normalised === 'running') return 'running';
  if (normalised === 'failed') return 'failed';
  if (normalised === 'stopped') return 'stopped';

  return 'unknown';
};
