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
import DocsHelper from 'util/DocsHelper';
import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { Button, Col, Row } from 'components/bootstrap';
import CreateNewProfile from 'components/indices/IndexSetFiledTypeProfiles/CreateNewProfile';
import useCurrentUser from 'hooks/useCurrentUser';

const IndexSetFieldTypeProfileCreatePage = () => {
  const navigate = useNavigate();
  const currentUser = useCurrentUser();

  useEffect(() => {
    const hasMappingPermission = currentUser.permissions.includes('typemappings:edit') || currentUser.permissions.includes('*');

    if (!hasMappingPermission) {
      navigate(Routes.NOTFOUND);
    }
  }, [currentUser.permissions, navigate]);

  return (
    <DocumentTitle title="Create Index Set Field Type Profile">
      <div>
        <PageHeader title="Create Index Set Field Type Profiles"
                    documentationLink={{
                      title: 'Index model documentation',
                      path: DocsHelper.PAGES.INDEX_MODEL,
                    }}
                    topActions={(
                      <LinkContainer to={Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.OVERVIEW}>
                        <Button bsStyle="info">Profiles</Button>
                      </LinkContainer>
                    )}>
          <span>
            You can create  field type profiles configuration.
          </span>
        </PageHeader>
        <Row className="content">
          <Col md={12}>
            <CreateNewProfile />
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default IndexSetFieldTypeProfileCreatePage;
