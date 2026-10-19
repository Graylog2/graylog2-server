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

import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';
import { Button } from 'components/bootstrap';
import { DataAdaptersOverview } from 'components/lookup-tables';
import { LUTPageLayout } from 'components/lookup-tables/layout-componets';
import { useTranslation } from 'react-i18next';

function LUTDataAdaptersPage() {
  const { push } = useHistory();
  const { t } = useTranslation('lookup-tables');

  return (
    <LUTPageLayout
      documentTitle={t('adapter_document_title')}
      pageTitle={t('adapter_page_title')}
      pageDescription={t('adapter_description')}
      actions={
        <Button bsStyle="primary" onClick={() => push(Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.CREATE)}>
          {t('adapter_create_button')}
        </Button>
      }>
      <DataAdaptersOverview />
    </LUTPageLayout>
  );
}

export default LUTDataAdaptersPage;
