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
import { Trans, useTranslation } from 'react-i18next';

import useProductName from 'brand-customization/useProductName';
import DocsHelper from 'util/DocsHelper';
import Section from 'preflight/components/common/Section';
import DataNodesOverview from 'preflight/components/Setup/DataNodesOverview';
import DocumentationLink from 'components/support/DocumentationLink';
import ConfigurationWizard from 'preflight/components/ConfigurationWizard';

const P = styled.p`
  max-width: 700px;
`;

// Trans renders the translated text of a tag as children, DocumentationLink renders its `text` prop instead.
const DocsLink = ({ page, children = null }: { page: string; children?: React.ReactNode }) => (
  <DocumentationLink page={page} text={children} />
);

type Props = {
  setIsWaitingForStartup: React.Dispatch<React.SetStateAction<boolean>>;
};

const Setup = ({ setIsWaitingForStartup }: Props) => {
  const { t } = useTranslation('preflight');
  const productName = useProductName();

  return (
    <>
      <Section title={t('setup.welcome')} titleOrder={1} dataTestid="welcome-section">
        <P>
          {t('setup.first_start', { productName })}
          <br />
          {t('setup.data_nodes_allow', { productName })}
        </P>
        <P>
          <Trans
            t={t}
            i18nKey="setup.data_node_setup"
            values={{ productName }}
            components={{
              dataNodeLink: <DocsLink page={DocsHelper.PAGES.GRAYLOG_DATA_NODE} />,
              openSearchLink: <DocsLink page={DocsHelper.PAGES.OPEN_SEARCH_SETUP} />,
            }}
          />
        </P>
      </Section>
      <Section title={t('setup.data_nodes_title', { productName })} titleOrder={2}>
        <DataNodesOverview />
        <ConfigurationWizard setIsWaitingForStartup={setIsWaitingForStartup} />
      </Section>
    </>
  );
};

export default Setup;
