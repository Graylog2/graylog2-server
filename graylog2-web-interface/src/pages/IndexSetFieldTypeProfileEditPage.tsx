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
import { Loader } from '@mantine/core';

import { DocumentTitle, PageHeader } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { Button, Col, Row } from 'components/bootstrap';
import useProfile from 'components/indices/IndexSetFiledTypeProfiles/hooks/useProfile';
import useParams from 'routing/useParams';
import EditProfile from 'components/indices/IndexSetFiledTypeProfiles/EditProfile';

const IndexSetFieldTypeProfileEditPage = () => {
  const { profileId } = useParams();
  const { data, isFetched } = useProfile(profileId);

  return (
    <DocumentTitle title="Edit Index Set Field Type Profile">
      <div>
        <PageHeader title={`Edit  "${data?.name}" Index Set Field Type Profiles`}
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
            {`You can modify "${data?.name}" field type profiles configuration.`}
          </span>
        </PageHeader>
        <Row className="content">
          <Col md={12}>
            {isFetched ? <EditProfile profile={data} /> : <Loader />}
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default IndexSetFieldTypeProfileEditPage;
