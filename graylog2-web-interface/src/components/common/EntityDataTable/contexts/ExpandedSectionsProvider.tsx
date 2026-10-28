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
import { useState, useCallback, useMemo } from 'react';

import ExpandedEntitiesSectionsContext from './ExpandedSectionsContext';

type ExpandedSections = { [entityId: string]: Array<string> };

const collapseRow = (cur: ExpandedSections | undefined, entityId: string) =>
  Object.fromEntries(Object.entries(cur ?? {}).filter(([id]) => id !== entityId));

const ExpandedSectionsProvider = ({ children }: { children: React.ReactNode }): React.ReactElement => {
  const [expandedSections, setExpandedSections] = useState<ExpandedSections | undefined>();

  // Opens the row on the given section, switches to it when the row shows another section,
  // or collapses the row when the section is already shown.
  const toggleSection = useCallback(
    (entityId: string, sectionName: string) =>
      setExpandedSections((cur) =>
        cur?.[entityId]?.includes(sectionName)
          ? collapseRow(cur, entityId)
          : { ...(cur ?? {}), [entityId]: [sectionName] },
      ),
    [],
  );

  const contextValue = useMemo(
    () => ({
      expandedSections,
      toggleSection,
    }),
    [expandedSections, toggleSection],
  );

  return (
    <ExpandedEntitiesSectionsContext.Provider value={contextValue}>{children}</ExpandedEntitiesSectionsContext.Provider>
  );
};

export default ExpandedSectionsProvider;
