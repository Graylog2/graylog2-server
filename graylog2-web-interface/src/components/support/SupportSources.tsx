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
import styled from 'styled-components';
import { Trans, useTranslation } from 'react-i18next';

import Icon from 'components/common/Icon';
import DocsHelper from 'util/DocsHelper';
import useProductName from 'brand-customization/useProductName';

import DocumentationLink from './DocumentationLink';

const SourcesList = styled.ul`
  margin: 0;
  padding: 0;
  margin-top: 5px;
`;

const SupportSources = () => {
  const { t } = useTranslation('common');
  const productName = useProductName();

  return (
    <div className="support-sources">
      <h2>{t('support_sources.title')}</h2>
      <p>
        <Trans
          t={t}
          i18nKey="support_sources.help_text"
          values={{ productName }}
          components={{
            docsLink: <DocumentationLink page={DocsHelper.PAGES.WELCOME} text={t('links.documentation')} />,
          }}
        />
      </p>

      <SourcesList>
        <li>
          <Icon name="group" />
          &nbsp;
          <a href="https://www.graylog.org/community-support/" target="_blank" rel="noopener noreferrer">
            {t('support_sources.community_support')}
          </a>
        </li>
        <li>
          <Icon name="bug_report" />
          &nbsp;&nbsp;
          {/* eslint-disable-next-line graylog/no-literal-product-name -- GitHub repository URL, not displayed text */}
          <a href="https://github.com/Graylog2/graylog2-server/issues" target="_blank" rel="noopener noreferrer">
            {t('support_sources.issue_tracker')}
          </a>
        </li>
        <li>
          <Icon name="favorite" />
          &nbsp;
          <a href="https://www.graylog.org/professional-support" target="_blank" rel="noopener noreferrer">
            {t('support_sources.professional_support')}
          </a>
        </li>
      </SourcesList>
    </div>
  );
};

export default SupportSources;
