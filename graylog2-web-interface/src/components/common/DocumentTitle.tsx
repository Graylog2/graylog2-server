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
import { useEffect } from 'react';

import useProductName from 'brand-customization/useProductName';

type Props = {
  title: React.ReactNode;
  children: React.ReactNode;
};

let titleOwner: symbol | null = null;

/**
 * React component that modifies the page `document.title` dynamically. When the component is unmounted, it
 * resets the title to the default (`Graylog`), unless another instance has taken over the title in the
 * meantime. During a route change React may mount the incoming page before unmounting the outgoing one, so
 * the outgoing cleanup must not clobber a title it no longer owns.
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

  useEffect(() => {
    const owner = Symbol('document-title');
    titleOwner = owner;
    document.title = `${productName} - ${title}`;

    return () => {
      if (titleOwner === owner) {
        titleOwner = null;
        document.title = productName;
      }
    };
  }, [productName, title]);

  return <>{children}</>;
};

export default DocumentTitle;
