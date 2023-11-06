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
import { useContext } from 'react';

export type IndexRetentionContextType = {
  useMaxNumberOfIndices: [
    number | undefined,
    React.Dispatch<React.SetStateAction<number>>
  ]
}

const IndexRetentionContext = React.createContext<IndexRetentionContextType | null>(null);

const useIndexRetention = (): IndexRetentionContextType => {
  try {
    const context = useContext(IndexRetentionContext);

    if (!context) {
      throw new Error('useIndexRetention must be used within a IndexRetentionProvider');
    }

    return context;
  } catch {
    return { useMaxNumberOfIndices: [9999, () => {}] };
  }
};

export { useIndexRetention };
export default IndexRetentionContext;
