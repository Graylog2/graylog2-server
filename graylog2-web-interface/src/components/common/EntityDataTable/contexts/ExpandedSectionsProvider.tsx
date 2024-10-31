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

const ExpandedSectionsProvider = ({ children }: { children: React.ReactNode }): React.ReactElement => {
  const [expandedSections, setExpandedSections] = useState<{ [entityId: string]: Array<string> } | undefined>();

  const toggleSection = useCallback((entityId: string, sectionName: string) => setExpandedSections((cur) => {
    const newCur = { ...(cur ?? {}) };

    if (newCur[entityId]?.includes(sectionName)) {
      const newSections = newCur[entityId].filter((section) => section !== sectionName);

      if (newSections.length === 0) {
        delete newCur[entityId];

        return newCur;
      }

      return { ...newCur, [entityId]: newSections };
    }

    return { ...newCur, [entityId]: [...(newCur[entityId] ?? []), sectionName] };
  }), []);

  const contextValue = useMemo(() => ({
    expandedSections,
    toggleSection,
  }), [expandedSections, toggleSection]);

  return (
    <ExpandedEntitiesSectionsContext.Provider value={contextValue}>
      {children}
    </ExpandedEntitiesSectionsContext.Provider>
  );
};

export default ExpandedSectionsProvider;
