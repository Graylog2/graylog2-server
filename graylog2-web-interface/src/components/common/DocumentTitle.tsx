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
import { useContext, useEffect } from 'react';

import useProductName from 'brand-customization/useProductName';

import DocumentTitleContext from './DocumentTitleContext';
import { registerDocumentTitle, unregisterDocumentTitle } from './documentTitleRegistry';

type Props = {
  title: React.ReactNode;
  children: React.ReactNode;
};

/**
 * React component that modifies the page `document.title` dynamically. Nested instances append their title
 * to the one of their parent (e.g. `Graylog - Alerts - Event Definitions`). When the component is unmounted,
 * the title of the remaining instances is restored, or the default (`Graylog`) if there is none.
 *
 * Example:
 *
 * ```js
 * <DocumentTitle title="This site is great">
 *   {contents}
 * </DocumentTitle>
 * ```
 */
const DocumentTitle = ({ children, title }: Props) => {
  const productName = useProductName();
  const parentTitles = useContext(DocumentTitleContext);
  const titles = [...parentTitles, `${title}`];
  const fullTitle = [productName, ...titles].join(' - ');
  const depth = titles.length;

  useEffect(() => {
    const id = Symbol('document-title');
    registerDocumentTitle(id, { depth, title: fullTitle }, productName);

    return () => unregisterDocumentTitle(id, productName);
  }, [depth, fullTitle, productName]);

  return <DocumentTitleContext.Provider value={titles}>{children}</DocumentTitleContext.Provider>;
};

export default DocumentTitle;
