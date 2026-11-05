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
import React from 'react';
import { useTranslation } from 'react-i18next';

import useParams from 'routing/useParams';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import { IndicesPageNavigation } from 'components/indices';
import EditTemplate from 'components/indices/IndexSetTemplates/EditTemplate';
import useTemplate from 'components/indices/IndexSetTemplates/hooks/useTemplate';

const IndexSetTemplateEditPage = () => {
  const { t } = useTranslation('indexsets');
  const { templateId } = useParams();
  const { data, isFetching, isSuccess, isError } = useTemplate(templateId);

  return (
    <DocumentTitle title={t('pages.template_edit_title')}>
      <IndicesPageNavigation />
      <PageHeader title={t('pages.template_edit_title')}>
        <span>{t('pages.template_edit_description')}</span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          {isFetching && <Spinner />}
          {isSuccess && <EditTemplate template={data} />}
          {isError && <p>{t('pages.template_load_error')}</p>}
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default IndexSetTemplateEditPage;
