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
import PropTypes from 'prop-types';
import styled from 'styled-components';

import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import Panel from 'components/graylog/Panel';

import Icon from './Icon';

const Header = styled(Panel.Title)`
  display: flex;
  align-items: center;
`;

const HeaderIcon = styled(Icon)`
  margin-right: 5px;
  margin-top: -1px;
`;

type Props = {
  featureName: string,
  wrapperClassName: string | null | undefined,
};

const EnterprisePluginNotFound = ({ featureName, wrapperClassName }: Props) => (
  <Panel bsStyle="info" className={wrapperClassName}>
    <Panel.Heading>
      <Header>
        <HeaderIcon name="crown" />Enterprise Feature
      </Header>
    </Panel.Heading>
    <Panel.Body>
      To use the <b>{featureName}</b> functionality you need the <a href="https://www.graylog.org/products/enterprise" rel="noopener noreferrer" target="_blank">Graylog Enterprise license</a> and the <DocumentationLink page={DocsHelper.PAGES.ENTERPRISE_SETUP} text="Graylog Enterprise plugin" />.
    </Panel.Body>
  </Panel>
);

EnterprisePluginNotFound.propTypes = {
  featureName: PropTypes.string.isRequired,
  wrapperClassName: PropTypes.string,
};

EnterprisePluginNotFound.defaultProps = {
  wrapperClassName: 'no-bm',
};

export default EnterprisePluginNotFound;
