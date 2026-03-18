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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import customColumnRenderers from './ColumnRenderers';

import type { CollectorInstanceView } from '../types';

const baseInstance: CollectorInstanceView = {
  id: 'inst-1',
  instance_uid: 'uid-1',
  capabilities: 15,
  fleet_id: 'fleet-1',
  enrolled_at: '2026-01-01T00:00:00Z',
  last_seen: '2026-03-17T12:00:00Z',
  certificate_fingerprint: 'aa:bb:cc',
  identifying_attributes: {},
  non_identifying_attributes: {},
  hostname: 'prod-web-01',
  os: 'linux',
  version: '1.2.0',
  status: 'online',
};

const fleetNames: Record<string, string> = {
  'fleet-1': 'Production',
  'fleet-2': 'Staging',
};

describe('Instance ColumnRenderers', () => {
  const renderers = customColumnRenderers({ fleetNames });

  describe('status', () => {
    it('renders Online label for online instances', async () => {
      render(<>{renderers.attributes.status.renderCell('online', baseInstance, {})}</>);

      await screen.findByText('Online');
    });

    it('renders Offline label for offline instances', async () => {
      const offlineInstance = { ...baseInstance, status: 'offline' as const };
      render(<>{renderers.attributes.status.renderCell('offline', offlineInstance, {})}</>);

      await screen.findByText('Offline');
    });
  });

  describe('hostname', () => {
    it('renders hostname when available', async () => {
      render(<>{renderers.attributes.hostname.renderCell('prod-web-01', baseInstance, {})}</>);

      await screen.findByText('prod-web-01');
    });

    it('falls back to instance_uid when hostname is null', async () => {
      const noHostname = { ...baseInstance, hostname: null };
      render(<>{renderers.attributes.hostname.renderCell(null, noHostname, {})}</>);

      await screen.findByText('uid-1');
    });
  });

  describe('os', () => {
    it('renders Linux icon', async () => {
      render(<>{renderers.attributes.os.renderCell('linux', baseInstance, {})}</>);

      await screen.findByTitle('Linux');
    });

    it('renders Unknown icon for null os', async () => {
      const noOs = { ...baseInstance, os: null };
      render(<>{renderers.attributes.os.renderCell(null, noOs, {})}</>);

      await screen.findByTitle('Unknown');
    });
  });

  describe('fleet_id', () => {
    it('renders fleet name as link', async () => {
      render(<>{renderers.attributes.fleet_id.renderCell('fleet-1', baseInstance, {})}</>);

      await screen.findByText('Production');
    });

    it('falls back to fleet_id when name not found', async () => {
      const unknownFleet = { ...baseInstance, fleet_id: 'fleet-unknown' };
      render(<>{renderers.attributes.fleet_id.renderCell('fleet-unknown', unknownFleet, {})}</>);

      await screen.findByText('fleet-unknown');
    });
  });
});
