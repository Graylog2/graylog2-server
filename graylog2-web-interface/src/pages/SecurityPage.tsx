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
import styled from 'styled-components';

import { IfPermitted, PageHeader } from 'components/common';
import { Alert } from 'components/bootstrap';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import AppConfig from 'util/AppConfig';

const StyledH4 = styled.h4`
  font-weight: bold;
  margin-bottom: 5px;
`;

const StyledAlert = styled(Alert)`
  margin-top: 15px;
`;

const isCloud = AppConfig.isCloud();

const SecurityPage = () => {
  return (
    <PageHeader title="Invalid License for Analyst Tools">
      <StyledAlert bsStyle="danger" className="tm">
        <StyledH4>Analyst Tools are disabled</StyledH4>
        <p>
          Analyst Tools are disabled because a valid Graylog for Security license was not found.
        </p>
        {isCloud
          ? (<>Contact your Graylog account manager.</>)
          : (
            <IfPermitted permissions="licenses:create">
              <p>
                See <Link to={Routes.pluginRoute('SYSTEM_LICENSES')}>Licenses page</Link> for details.
              </p>
            </IfPermitted>
          )}
      </StyledAlert>
    </PageHeader>
  );
};

export default SecurityPage;
