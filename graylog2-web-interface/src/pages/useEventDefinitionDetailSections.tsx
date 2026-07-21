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
import type * as React from 'react';
import { useMemo } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';

export type DetailPageSection = {
  key: string;
  component: React.ComponentType<{ eventDefinition: EventDefinition }>;
  order?: number;
};

const useEventDefinitionDetailSections = (): DetailPageSection[] => {
  const sections = usePluginEntities('eventDefinitions.components.detailPageSections');

  return useMemo(() => {
    if (!sections || sections.length === 0) {
      return [];
    }

    return [...sections].sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
  }, [sections]);
};

export default useEventDefinitionDetailSections;
