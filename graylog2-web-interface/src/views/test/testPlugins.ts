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
import type { PluginRegistration, PluginExports } from 'graylog-web-plugin/plugin';
import { PluginStore, PluginManifest } from 'graylog-web-plugin/plugin';

export const loadPlugin = (plugin: PluginRegistration) => PluginStore.register(plugin);
export const unloadPlugin = (plugin: PluginRegistration) => PluginStore.unregister(plugin);

export const usePlugin = (plugin: PluginRegistration) => {
  beforeAll(() => loadPlugin(plugin));
  afterAll(() => unloadPlugin(plugin));
};

export const usePluginExports = (exports: PluginExports) => usePlugin(new PluginManifest({}, exports));
