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
import type { PropsWithChildren } from 'react';
import { useCallback, useMemo, useState } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import type { Perspective } from 'components/perspectives/types';
import usePersistedSetting from 'hooks/usePersistedSetting';

import PerspectivesContext from './PerspectivesContext';

const DEFAULT_PERSPECTIVE = 'default';

const findPerspective = (perspectives: Array<Perspective>, perspectiveId: string) =>
  perspectives.find(({ id }) => id === perspectiveId);

const useActivePerspectiveState = (availablePerspectives: Array<Perspective>) => {
  const [persistedPerspective, setPersistedPerspective] = usePersistedSetting('perspective');
  const [activePerspective, setActivePerspective] = useState<string>(
    findPerspective(availablePerspectives, persistedPerspective) ? persistedPerspective : DEFAULT_PERSPECTIVE,
  );
  const setActivePerspectiveWithPersistence = useCallback(
    (newPerspective: string) => {
      setActivePerspective(newPerspective);

      return setPersistedPerspective(newPerspective);
    },
    [setPersistedPerspective],
  );

  return {
    activePerspective: findPerspective(availablePerspectives, activePerspective),
    setActivePerspective: setActivePerspectiveWithPersistence,
  };
};

const PerspectivesProvider = ({ children }: PropsWithChildren) => {
  const allPerspectives = usePluginEntities('perspectives');
  const availablePerspectives = allPerspectives.filter((perspective) =>
    perspective.useCondition ? !!perspective.useCondition() : true,
  );
  const { activePerspective, setActivePerspective } = useActivePerspectiveState(availablePerspectives);
  const contextValue = useMemo(
    () => ({
      activePerspective,
      availablePerspectives,
      setActivePerspective,
    }),
    [activePerspective, availablePerspectives, setActivePerspective],
  );

  return <PerspectivesContext.Provider value={contextValue}>{children}</PerspectivesContext.Provider>;
};

export default PerspectivesProvider;
