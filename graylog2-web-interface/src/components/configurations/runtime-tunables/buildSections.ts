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

import type { Permission } from 'graylog-web-plugin/plugin';

import type { CoreSystemConfiguration } from 'views/types';

import FeatureSection from './FeatureSection';
import type { RuntimeTunableFeatureSummary } from './types';

type Args = {
  features: Array<RuntimeTunableFeatureSummary>;
  coreConfigurations: Array<CoreSystemConfiguration>;
  isPermitted: (permissions: Array<Permission> | Permission) => boolean;
};

const buildSections = ({ features, coreConfigurations, isPermitted }: Args): Array<CoreSystemConfiguration> => {
  const permittedCore = coreConfigurations.filter(({ permissions }) => (permissions ? isPermitted(permissions) : true));
  const featureIds = features.map(({ id }) => id);

  const featureSections: Array<CoreSystemConfiguration> = features
    .map((feature) => ({
      feature,
      core: permittedCore.filter((configuration) => configuration.feature === feature.id),
    }))
    .filter(({ feature, core }) => core.length > 0 || feature.tunableCount > 0)
    .map(({ feature, core }) => ({
      name: feature.title,
      SectionComponent: FeatureSection,
      props: { feature, coreConfigurations: core },
    }));

  const unassigned = permittedCore.filter(({ feature }) => !feature || !featureIds.includes(feature));

  return [...featureSections, ...unassigned];
};

export default buildSections;
