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
import { PluginStore } from 'graylog-web-plugin/plugin';

import MissingRequirements from 'views/components/views/MissingRequirements';
import type { ViewHook } from 'views/logic/hooks/ViewHook';
import type { Requirements } from 'views/logic/views/View';

const _missingRequirements = (requirements, requirementsProvided): Requirements => Object.entries(requirements)
  .filter(([require]) => !requirementsProvided.includes(require))
  .reduce((prev, [key, value]) => ({ ...prev, [key]: value }), {});

const RequirementsProvided: ViewHook = ({ view }) => {
  return new Promise((resolve, reject) => {
    const providedRequirements = PluginStore.exports('views.requires.provided');
    const missingRequirements = _missingRequirements(view.requires, providedRequirements);

    if (Object.keys(missingRequirements).length > 0) {
      const Component = () => <MissingRequirements view={view} missingRequirements={missingRequirements} />;

      return reject(Component);
    }

    return resolve(true);
  });
};

export default RequirementsProvided;
