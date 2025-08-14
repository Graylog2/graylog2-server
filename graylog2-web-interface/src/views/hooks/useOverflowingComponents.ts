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
import { useContext, useMemo, useState } from 'react';

import OverflowingComponentsContext from 'views/components/contexts/OverflowingComponentsContext';
import type { ActionComponents } from 'views/components/actions/ActionHandler';

const useOverflowingComponents = () => {
  const [selfOverflowingComponents, setSelfOverflowingComponents] = useState<ActionComponents>({});
  const context = useContext(OverflowingComponentsContext);
  const overflowingComponents = useMemo(
    () => context?.overflowingComponents ?? selfOverflowingComponents,
    [context?.overflowingComponents, selfOverflowingComponents],
  );
  const setOverflowingComponents = useMemo(
    () => context?.setOverflowingComponents ?? setSelfOverflowingComponents,
    [context?.setOverflowingComponents],
  );

  return { isFromContext: !!context, overflowingComponents, setOverflowingComponents };
};

export default useOverflowingComponents;
