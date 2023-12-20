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
import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import { DocumentTitle, PageHeader } from 'components/common';
import { Row, Col, Button } from 'components/bootstrap';
import DocsHelper from 'util/DocsHelper';
import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import useCurrentUser from 'hooks/useCurrentUser';
import IndexSetFieldTypeProfilesList from 'components/indices/IndexSetFiledTypeProfiles/IndexSetFieldTypeProfilesList';

const IndexSetFieldTypeProfilesPage = () => {
  const navigate = useNavigate();
  const currentUser = useCurrentUser();

  useEffect(() => {
    const hasMappingPermission = currentUser.permissions.includes('typemappings:edit') || currentUser.permissions.includes('*');

    if (!hasMappingPermission) {
      navigate(Routes.NOTFOUND);
    }
  }, [currentUser.permissions, navigate]);

  return (
    <DocumentTitle title="Index Set Field Type Profiles">
      <div>
        <PageHeader title="Configure Index Set Field Type Profiles"
                    documentationLink={{
                      title: 'Index model documentation',
                      path: DocsHelper.PAGES.INDEX_MODEL,
                    }}
                    topActions={(
                      <LinkContainer to={Routes.SYSTEM.INDICES.LIST}>
                        <Button bsStyle="info">Index sets</Button>
                      </LinkContainer>
                    )}>
          <span>
            You can modify the current field type profiles configuration or create the new one.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <IndexSetFieldTypeProfilesList />
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default IndexSetFieldTypeProfilesPage;
