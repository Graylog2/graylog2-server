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

import useParams from 'routing/useParams';
import Routes from 'routing/Routes';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { LinkContainer } from 'components/common/router';
import { Button, ButtonToolbar, Col, Row } from 'components/bootstrap';
import { IndicesPageNavigation } from 'components/indices';
import TemplateDetails from 'components/indices/IndexSetTemplates/TemplateDetails';
import useTemplate from 'components/indices/IndexSetTemplates/hooks/useTemplate';

const IndexSetTemplatePage = () => {
  const { templateId } = useParams();
  const { data, isFetching, isSuccess, isError } = useTemplate(templateId);

  const title = !isFetching ? data.title : 'Index Set Template';
  const description = !isFetching ? data.description : 'Viewing Index Set Template Details';

  return (
    <DocumentTitle title={title}>
      <IndicesPageNavigation />
      <PageHeader title={title}
                  actions={(
                    <ButtonToolbar>
                      {!isFetching && !data.built_in && (
                        <LinkContainer to={Routes.SYSTEM.INDICES.TEMPLATES.edit(templateId)}>
                          <Button bsStyle="success">Edit</Button>
                        </LinkContainer>
                      )}
                      <LinkContainer to={Routes.SYSTEM.INDICES.TEMPLATES.OVERVIEW}>
                        <Button>Overview</Button>
                      </LinkContainer>
                    </ButtonToolbar>
                  )}>
        <span>
          {description}
        </span>

      </PageHeader>
      <Row className="content">
        <Col md={12}>
          {isFetching && <Spinner />}
          {isSuccess && <TemplateDetails template={data} />}
          {isError && <p>There was an error when loading the template.</p>}
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default IndexSetTemplatePage;
