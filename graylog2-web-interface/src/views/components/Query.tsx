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
import { useTranslation, Trans } from 'react-i18next';

import DocsHelper from 'util/DocsHelper';
import { Jumbotron } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';
import WidgetGrid from 'views/components/WidgetGrid';
import useWidgets from 'views/hooks/useWidgets';
import usePluggableUpsellWrapper from 'hooks/usePluggableUpsellWrapper';

const NoWidgetsInfo = () => {
  const UpsellWrapper = usePluggableUpsellWrapper();
  const { t } = useTranslation('search');

  return (
    <Jumbotron>
      <h2>
        <IfDashboard>{t('empty_state.no_widgets_dashboard')}</IfDashboard>
        <IfSearch>{t('empty_state.no_widgets_search')}</IfSearch>
      </h2>
      <br />
      <p>
        {t('empty_state.create_widget_hint')}
        <br />
      </p>
      <p>{t('empty_state.tips_title')}</p>
      <ul>
        <li>
          <p>
            1. <Trans i18nKey="empty_state.tip_1" ns="search" components={{ bold: <b /> }} />
          </p>
        </li>
        <li>
          <p>
            2. <Trans i18nKey="empty_state.tip_2" ns="search" components={{ bold: <b /> }} />
          </p>
        </li>
        <li>
          <p>
            3. <Trans i18nKey="empty_state.tip_3" ns="search" components={{ bold: <b /> }} />
          </p>
        </li>
        <IfDashboard>
          <li>
            <p>
              4. <Trans i18nKey="empty_state.tip_4" ns="search" components={{ bold: <b /> }} />
              <UpsellWrapper>
                {' '}
                (contained in{' '}
                <a href="https://www.graylog.org/graylog-enterprise-edition" target="_blank" rel="noopener noreferrer">
                  Graylog Enterprise
                </a>
                )
              </UpsellWrapper>
              .
            </p>
          </li>
        </IfDashboard>
      </ul>
      <p>
        You can also have a look at the <DocumentationLink page={DocsHelper.PAGES.DASHBOARDS} text="documentation" />,
        to learn more about the widget creation.
      </p>
    </Jumbotron>
  );
};

const useHasWidgets = () => {
  const widgets = useWidgets();

  return widgets?.size > 0;
};

const Query = () => {
  const hasWidgets = useHasWidgets();

  return hasWidgets ? <WidgetGrid /> : <NoWidgetsInfo />;
};

const memoizedQuery = React.memo(Query);
memoizedQuery.displayName = 'Query';

export default memoizedQuery;
