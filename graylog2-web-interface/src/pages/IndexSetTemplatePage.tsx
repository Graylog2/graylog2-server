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
import Routes from 'routing/Routes';
import { DocumentTitle, PageHeader, Spinner, IfPermitted, LinkContainer } from 'components/common';
import { Button, ButtonToolbar, Col, Row } from 'components/bootstrap';
import { IndicesPageNavigation } from 'components/indices';
import TemplateDetails from 'components/indices/IndexSetTemplates/TemplateDetails';
import useTemplate from 'components/indices/IndexSetTemplates/hooks/useTemplate';

const IndexSetTemplatePage = () => {
  const { t } = useTranslation('indexsets');
  const { templateId } = useParams();
  const { data, isFetching, isSuccess, isError } = useTemplate(templateId);

  const title = !isFetching ? data.title : t('pages.template_title');
  const description = !isFetching ? data.description : t('pages.template_view_description');

  return (
    <DocumentTitle title={title}>
      <IndicesPageNavigation />
      <PageHeader
        title={title}
        actions={
          <ButtonToolbar>
            {!isFetching && !data.built_in && (
              <IfPermitted permissions="indexset_templates:edit">
                <LinkContainer to={Routes.SYSTEM.INDICES.TEMPLATES.edit(templateId)}>
                  <Button bsStyle="primary">{t('common:actions.edit')}</Button>
                </LinkContainer>
              </IfPermitted>
            )}
            <LinkContainer to={Routes.SYSTEM.INDICES.TEMPLATES.OVERVIEW}>
              <Button>{t('pages.template_overview')}</Button>
            </LinkContainer>
          </ButtonToolbar>
        }>
        <span>{description}</span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          {isFetching && <Spinner />}
          {isSuccess && <TemplateDetails template={data} />}
          {isError && <p>{t('pages.template_load_error')}</p>}
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default IndexSetTemplatePage;
