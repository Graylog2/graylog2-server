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
import { useContext, useEffect, useState } from 'react';

import DocumentTitleContext from 'contexts/DocumentTitleContext';

const useDocumentTitle = (title: string) => {
  const registry = useContext(DocumentTitleContext);

  if (!registry) {
    throw new Error('useDocumentTitle needs to be used inside DocumentTitleContextProvider.');
  }

  // Reserved during render, where parents come before their children, unlike effects.
  const [position] = useState(registry.reservePosition);

  useEffect(() => registry.register(title, position), [registry, title, position]);
};

export default useDocumentTitle;
