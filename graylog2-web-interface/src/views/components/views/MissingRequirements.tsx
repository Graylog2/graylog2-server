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
import { Trans, useTranslation } from 'react-i18next';

import ViewTypeLabel from 'views/components/ViewTypeLabel';
import View from 'views/logic/views/View';
import type { PluginMetadata, Requirements } from 'views/logic/views/View';
import { Col, Row, Button } from 'components/bootstrap';
import fixup from 'views/pages/StyleFixups.css';
import useHistory from 'routing/useHistory';

type Props = {
  view: View;
  missingRequirements: Requirements;
};

// `Trans` provides the link text as children.
const PluginLink = ({ url, children = undefined }: React.PropsWithChildren<{ url: string }>) => (
  <a href={url} target="_blank" rel="noopener noreferrer">
    {children}
  </a>
);

const MissingRequirements = ({ view, missingRequirements }: Props) => {
  const { t } = useTranslation('search');
  const history = useHistory();

  return (
    <Row className="content">
      <Col md={6} mdOffset={3} className={fixup.bootstrapHeading}>
        <h1>
          {ViewTypeLabel({ type: view.type, capitalize: true })}: <em>{view.title}</em>
        </h1>
        <p>
          {view.type === View.Type.Dashboard
            ? t('missing_requirements.description_dashboard')
            : t('missing_requirements.description_search')}
        </p>

        <ul>
          {Object.entries(missingRequirements).map(([require, plugin]: [string, PluginMetadata]) => (
            <li key={require}>
              <Trans
                t={t}
                i18nKey="missing_requirements.included_in"
                values={{ requirement: require, plugin: plugin.name }}
                components={{
                  requirementName: <strong />,
                  pluginLink: <PluginLink url={plugin.url} />,
                }}
              />
            </li>
          ))}
        </ul>
      </Col>

      <Col md={1} mdOffset={8}>
        <Button bsStyle="primary" onClick={() => history.goBack()}>
          {t('missing_requirements.back')}
        </Button>
      </Col>
    </Row>
  );
};

export default MissingRequirements;
