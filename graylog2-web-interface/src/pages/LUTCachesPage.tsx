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

import Routes from 'routing/Routes';
import { Button } from 'components/bootstrap';
import { CachesOverview } from 'components/lookup-tables';
import { LUTPageLayout } from 'components/lookup-tables/layout-componets';

function LUTCachesPage() {
  return (
    <LUTPageLayout
      documentTitle="Lookup Tables - Caches"
      pageTitle="Caches for Lookup Tables"
      pageDescription="Caches provide the actual values for lookup tables."
      actions={
        <Button bsStyle="primary" href={Routes.SYSTEM.LOOKUPTABLES.CACHES.CREATE}>
          Create cache
        </Button>
      }>
      <CachesOverview />
    </LUTPageLayout>
  );
}

export default LUTCachesPage;
