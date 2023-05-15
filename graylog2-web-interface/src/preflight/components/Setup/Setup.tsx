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
import { Title, Space } from '@mantine/core';
import styled from 'styled-components';

import DocsHelper from 'util/DocsHelper';
import Section from 'preflight/components/common/Section';
import DataNodesOverview from 'preflight/components/Setup/DataNodesOverview';
import DocumentationLink from 'components/support/DocumentationLink';

const P = styled.p`
  max-width: 700px;
`;

type Props = {
  onResumeStartup: () => void,
}

const Setup = ({ onResumeStartup }: Props) => (
  <Section title="Welcome!" titleOrder={1}>
    <P>
      It looks like you are starting Graylog for the first time and have not configured a data node.<br />
      Data nodes allow you to index and search through all the messages in your Graylog message database.
    </P>
    <P>
      You can either implement a <DocumentationLink page={DocsHelper.PAGES.GRAYLOG_DATA_NODE} text="Graylog data node" /> (recommended) or you can configure an <DocumentationLink page={DocsHelper.PAGES.OPEN_SEARCH_SETUP} text="OpenSearch" /> node manually.
    </P>

    <Space h="md" />
    <Title order={2}>Graylog Data Nodes</Title>
    <DataNodesOverview onResumeStartup={onResumeStartup} />

    <Space h="md" />
    <Title order={2}>Manual Data Node Configuration</Title>
    <P>
      If you want to configure an Elasticsearch or OpenSearch node manually, you need to adjust the Graylog configuration and restart the Graylog server.
      After the restart this page will not show up again.
    </P>
  </Section>
);

export default Setup;
