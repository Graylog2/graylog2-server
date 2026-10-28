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
type Fragment = {
  position: number;
  title: string;
};

const createDocumentTitleRegistry = (productName: string) => {
  const fragments = new Map<symbol, Fragment>();
  let lastPosition = 0;

  const updateDocumentTitle = () => {
    const titles = [...fragments.values()]
      .sort((fragment1, fragment2) => fragment1.position - fragment2.position)
      .map(({ title }) => title)
      .filter((title) => title.trim() !== '');

    document.title = [productName, ...titles].join(' - ');
  };

  return {
    reservePosition: () => {
      lastPosition += 1;

      return lastPosition;
    },
    register: (title: string, position: number) => {
      const id = Symbol('document-title');
      fragments.set(id, { position, title });
      updateDocumentTitle();

      return () => {
        fragments.delete(id);
        updateDocumentTitle();
      };
    },
  };
};

export type DocumentTitleRegistry = ReturnType<typeof createDocumentTitleRegistry>;

export default createDocumentTitleRegistry;
