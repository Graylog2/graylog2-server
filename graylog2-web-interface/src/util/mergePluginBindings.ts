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
import mergeWith from 'lodash/mergeWith';
import type { PluginExports } from 'graylog-web-plugin/plugin';

type MergeablePluginBindings = PluginExports | Record<string, unknown>;

// Concatenate array-valued keys instead of merging them by index, while letting lodash
// deep-merge everything else. Returning `undefined` falls back to lodash's default behaviour.
const concatArrays = (objValue: unknown, srcValue: unknown) =>
  Array.isArray(objValue) ? objValue.concat(srcValue) : undefined;

/**
 * Merge multiple plugin binding objects (`PluginExports`) into a single one.
 *
 * Plugin bindings are keyed by extension point, and most values are arrays of entries
 * (e.g. `pageNavigation`, `routes`, `eventDefinitionTypes`). When several (sub-)plugins
 * contribute to the same extension point, their entries must be **concatenated** so each
 * one is preserved as a separate entry.
 *
 * A naive `lodash/merge` merges arrays by index, which fuses unrelated entries together
 * (e.g. a base `pageNavigation` entry with another plugin's nav injection). This helper
 * uses `mergeWith` with a customizer that concatenates array-valued keys instead, and
 * merges into a fresh `{}` target so none of the source bindings are mutated. Object-valued
 * keys are still deep-merged, preserving keys that only exist in some of the bindings.
 *
 * @param bindings The plugin binding objects to merge, in order of precedence.
 * @returns A new, merged `PluginExports` object.
 */
const mergePluginBindings = (...bindings: Array<MergeablePluginBindings>): PluginExports =>
  mergeWith({}, ...bindings, concatArrays) as PluginExports;

export default mergePluginBindings;
