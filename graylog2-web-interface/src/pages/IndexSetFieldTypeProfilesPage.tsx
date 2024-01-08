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

import { isPermitted } from 'util/PermissionsMixin';
import { DocumentTitle, PageHeader } from 'components/common';
import { Row, Col } from 'components/bootstrap';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import useCurrentUser from 'hooks/useCurrentUser';
import ProfilesList from 'components/indices/IndexSetFiledTypeProfiles/ProfilesList';
import CreateProfileButton from 'components/indices/IndexSetFiledTypeProfiles/CreateProfileButton';
import { IndicesPageNavigation } from 'components/indices';

const IndexSetFieldTypeProfilesPage = () => {
  const navigate = useNavigate();
  const currentUser = useCurrentUser();
  const hasMappingPermission = isPermitted(currentUser.permissions, 'typemappings:edit');

  useEffect(() => {
    if (!hasMappingPermission) {
      navigate(Routes.NOTFOUND);
    }
  }, [hasMappingPermission, navigate]);

  return (
    <DocumentTitle title="Index Set Field Type Profiles">
      <IndicesPageNavigation />
      <div>
        <PageHeader title="Configure Index Set Field Type Profiles"
                    documentationLink={{
                      title: 'Index model documentation',
                      path: DocsHelper.PAGES.INDEX_MODEL,
                    }}
                    actions={<CreateProfileButton />}>
          <span>
            You can modify the current field type profiles configuration or create the new one.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <ProfilesList />
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default IndexSetFieldTypeProfilesPage;
