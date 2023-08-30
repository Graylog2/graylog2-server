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
import { useMemo, useState } from 'react';

import RuleBuilderContext from './RuleBuilderContext';
import type { RuleBuilderContextType } from './RuleBuilderContext';

type Props = {
  children: React.ReactNode,
};

const RuleBuilderProvider = ({ children }: Props) => {
  const [highlightedOutput, setHighlightedOutput] = useState<string>(undefined);

  const value: RuleBuilderContextType = useMemo(() => ({ useHighlightedOutput: [highlightedOutput, setHighlightedOutput] }), [highlightedOutput]);

  return (
    <RuleBuilderContext.Provider value={value}>
      {children}
    </RuleBuilderContext.Provider>
  );
};

export default RuleBuilderProvider;
