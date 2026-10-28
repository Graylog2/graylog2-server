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
import { useTranslation } from 'react-i18next';

import { DocumentTitle } from 'components/common';
import { Alert, Row, Col } from 'components/bootstrap';
import PageHeader from 'components/common/PageHeader';
import PageContentLayout from 'components/layout/PageContentLayout';

const UserHasNoStreamAccess = () => {
  const { t } = useTranslation('streams');

  return (
    <DocumentTitle title={t('user_no_stream_access.title')}>
      <PageContentLayout>
        <PageHeader title={t('user_no_stream_access.title')} />
        <Row className="content">
          <Col md={12}>
            <Alert bsStyle="warning">
              {t('user_no_stream_access.alert')}
            </Alert>
          </Col>
        </Row>
      </PageContentLayout>
    </DocumentTitle>
  );
};

export default UserHasNoStreamAccess;
