// @flow strict
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
