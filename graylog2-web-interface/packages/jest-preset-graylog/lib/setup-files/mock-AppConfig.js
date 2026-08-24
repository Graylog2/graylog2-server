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
const mockAppConfig = {
  contentStream: jest.fn(),
  features: undefined,
  gl2ServerUrl: jest.fn(() => 'http://localhost:9000/api/'),
  gl2AppPathPrefix: jest.fn(() => '/'),
  gl2DevMode: jest.fn(() => false),
  isFeatureEnabled: jest.fn(() => true),
  rootTimeZone: jest.fn(() => 'UTC'),
  isCloud: jest.fn(() => false),
  customThemeColors: jest.fn(() => ({})),
  telemetry: jest.fn(),
  publicNotifications: jest.fn(() => ({})),
  pluginUISettings: jest.fn(() => ({})),
  branding: jest.fn(),
  globalInputsOnly: jest.fn(() => false),
};

jest.mock('util/AppConfig', () => mockAppConfig);
