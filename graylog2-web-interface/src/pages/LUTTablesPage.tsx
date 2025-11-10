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

import { Button } from 'components/bootstrap';
import { LookupTablesOverview } from 'components/lookup-tables';
import { LUTPageLayout } from 'components/lookup-tables/layout-componets';
import { useModalContext } from 'components/lookup-tables/contexts/ModalContext';
import LUTModals from 'components/lookup-tables/LUTModals';

function LUTTablesPage() {
  const { setModal } = useModalContext();

  const showCreateModal = () => {
    setModal('LUT-CREATE');
  };

  return (
    <LUTPageLayout
      documentTitle="Lookup Tables"
      pageTitle="Lookup Tables"
      pageDescription="Lookup tables can be used in extractors, converters and processing pipelines to translate message fields or to enrich messages."
      actions={
        <Button bsStyle="primary" onClick={showCreateModal}>
          Create lookup table
        </Button>
      }>
      <LookupTablesOverview />
      <LUTModals />
    </LUTPageLayout>
  );
}

export default LUTTablesPage;
