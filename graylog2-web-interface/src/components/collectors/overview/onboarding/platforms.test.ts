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
import PLATFORMS from './platforms';

const commandFor = (id: string) => {
  const platform = PLATFORMS.find((p) => p.id === id);

  if (!platform) throw new Error(`unknown platform ${id}`);

  return platform.commandTemplate('https://graylog.example.com', 'tok-123');
};

describe('PLATFORMS command templates', () => {
  it('linux pipes the download script into sudo sh with endpoint and token', () => {
    expect(commandFor('linux')).toBe(
      'curl -fsSL https://downloads.graylog.org/repo/scripts/collector/install-linux.sh | sudo sh -s -- --endpoint https://graylog.example.com --token tok-123',
    );
  });

  it('macos pipes the download script into sudo sh with endpoint and token', () => {
    expect(commandFor('macos')).toBe(
      'curl -fsSL https://downloads.graylog.org/repo/scripts/collector/install-macos.sh | sudo sh -s -- --endpoint https://graylog.example.com --token tok-123',
    );
  });

  it('windows runs the downloaded script as a scriptblock with endpoint and token', () => {
    expect(commandFor('windows')).toBe(
      '& ([scriptblock]::Create((irm https://downloads.graylog.org/repo/scripts/collector/install-windows.ps1))) -Endpoint https://graylog.example.com -Token tok-123',
    );
  });
});
