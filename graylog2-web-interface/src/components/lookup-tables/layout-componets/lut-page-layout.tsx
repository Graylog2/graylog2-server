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

import PageNavigation from 'components/common/PageNavigation';
import { Row } from 'components/bootstrap';
import { PageHeader, DocumentTitle } from 'components/common';
import { PAGE_NAV_TITLE } from 'components/lookup-tables/bindings';

type Props = {
  documentTitle?: string;
  pageTitle?: string;
  pageDescription?: string;
  actions?: React.ReactElement;
  children: React.ReactNode;
};

function LUTLayout({
  documentTitle = undefined,
  pageTitle = undefined,
  pageDescription = undefined,
  actions = null,
  children,
}: Props) {
  return (
    <DocumentTitle title={documentTitle}>
      <Row>
        <PageNavigation page={PAGE_NAV_TITLE} />
      </Row>
      <PageHeader title={pageTitle} actions={actions}>
        <span>{pageDescription}</span>
      </PageHeader>
      {children}
    </DocumentTitle>
  );
}

export default LUTLayout;
