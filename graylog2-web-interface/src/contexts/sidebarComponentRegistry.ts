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

const registry = new Map<string, React.ComponentType<any>>();

export const registerSidebarComponent = (key: string, component: React.ComponentType<any>) => {
  registry.set(key, component);
};

export const getSidebarComponent = (key: string): React.ComponentType<any> => {
  const component = registry.get(key);

  if (!component) {
    throw new Error(`Sidebar component "${key}" is not registered. Ensure the module that registers it has been imported.`);
  }

  return component;
};
