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
import collectorReceivedMessagesUrl from './collectorReceivedMessagesUrl';

describe('collectorReceivedMessagesUrl', () => {
  it('builds a search URL with a quoted field:value query', () => {
    const url = collectorReceivedMessagesUrl('collector_instance_uid', 'uid-42');

    expect(url).toContain('/search?');
    expect(url).toContain('q=collector_instance_uid%3A%22uid-42%22');
  });

  it('uses a 1h relative time range', () => {
    const url = collectorReceivedMessagesUrl('collector_fleet_id', 'fleet-xyz');

    expect(url).toContain('rangetype=relative');
    expect(url).toContain('relative=3600');
  });

  it('does not include a streams parameter', () => {
    const url = collectorReceivedMessagesUrl('collector_source_id', 'source-abc');

    expect(url).not.toContain('streams=');
  });

  it('quotes values that contain special characters', () => {
    const url = collectorReceivedMessagesUrl('collector_source_id', 'id with space');

    // Quoting the value protects against special characters in the query string.
    // After URL encoding: "id with space" → %22id%20with%20space%22 (or + for space).
    expect(url).toMatch(/q=collector_source_id%3A%22id(?:%20|\+)with(?:%20|\+)space%22/);
  });
});
