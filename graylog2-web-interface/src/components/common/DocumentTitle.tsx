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

import useDocumentTitle from 'hooks/useDocumentTitle';

type Props = {
  title: React.ReactNode;
  children: React.ReactNode;
};

/**
 * React component that modifies the page `document.title` dynamically. Nested instances append their title
 * to the one of their parent (e.g. `Graylog - Alerts - Event Definitions`). When the component is unmounted,
 * its title is removed again.
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
  useDocumentTitle(`${title}`);

  return <>{children}</>;
};

export default DocumentTitle;
