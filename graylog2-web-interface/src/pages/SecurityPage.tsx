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

import { IfPermitted } from 'components/common';
import { Alert } from 'components/bootstrap';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import AppConfig from 'util/AppConfig';

const StyledH2 = styled.h2`
  font-weight: bold;
  margin-bottom: 15px;
`;

const StyledH4 = styled.h4`
  font-weight: bold;
  margin-bottom: 10px;
`;

const StyledAlert = styled(Alert)`
  margin-top: 15px;
`;

const isCloud = AppConfig.isCloud();

const LinkTo = () => {
  if (Routes.pluginRoute('SYSTEM_LICENSES', false)) {
    return (
      <IfPermitted permissions="licenses:create">
        <p>
          See <Link to={Routes.pluginRoute('SYSTEM_LICENSES')}>Licenses page</Link> for details.
        </p>
      </IfPermitted>
    );
  }

  return (
    <p>
      Please see <a href="https://www.graylog.org/products/enterprise" rel="noopener noreferrer" target="_blank">our product page</a> for details.
    </p>
  );
};

const SecurityPage = () => {
  return (
    <StyledAlert bsStyle="danger" className="tm">
      <StyledH2>Invalid License for the Security plugin</StyledH2>
      <StyledH4>Security plugin is disabled</StyledH4>
      <p>
        The security plugin is disabled because a valid Graylog for Security license was not found{Routes.pluginRoute('SYSTEM_LICENSES', false) ? '' : ' and the enterprise plugin is not installed'}.
      </p>
      {isCloud
        ? (<>Contact your Graylog account manager.</>)
        : (<LinkTo />)}
    </StyledAlert>
  );
};

export default SecurityPage;
