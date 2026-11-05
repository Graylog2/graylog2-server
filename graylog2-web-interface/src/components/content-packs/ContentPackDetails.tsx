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
import { useTranslation } from 'react-i18next';

import { Col, Row, Well } from 'components/bootstrap';
import ContentPackStatus from 'components/content-packs/ContentPackStatus';
import ContentPackConstraints from 'components/content-packs/ContentPackConstraints';
import ContentPackEntitiesList from 'components/content-packs/ContentPackEntitiesList';
import ContentPackParameterList from 'components/content-packs/ContentPackParameterList';
import 'components/content-packs/ContentPackDetails.css';
import { hasAcceptedProtocol } from 'util/URLUtils';
import Markdown from 'components/common/Markdown';

type ContentPackDetailsProps = {
  contentPack: any;
  constraints?: any[];
  verbose?: boolean;
  offset?: number;
  showConstraints?: boolean;
};

const ContentPackDetails = ({
  contentPack,
  offset = 1,
  verbose = false,
  constraints = [],
  showConstraints = false,
}: ContentPackDetailsProps) => {
  const { t } = useTranslation('content_packs');
  let contentPackAnchor = contentPack.url;

  try {
    if (hasAcceptedProtocol(contentPack.url)) {
      contentPackAnchor = <a href={contentPack.url}>{contentPack.url}</a>;
    }
  } catch (_e) {
    // Do nothing
  }

  return (
    <Row>
      <Col smOffset={offset} sm={9}>
        <div id="content-pack-details">
          <h2>{t('details.title')}</h2>
          <br />
          <div>
            <dl className="deflist">
              <dt>{t('details.version')}</dt> <dd>{contentPack.rev}</dd>
              <dt>{t('details.name')}</dt> <dd>{contentPack.name}&nbsp;</dd>
              <dt>{t('details.summary')}</dt> <dd>{contentPack.summary}&nbsp;</dd>
              <dt>{t('details.vendor')}</dt> <dd>{contentPack.vendor}&nbsp;</dd>
              <dt>{t('details.url')}</dt> <dd>{contentPackAnchor}&nbsp;</dd>
              {contentPack.id && (
                <span>
                  <dt>{t('details.id')}</dt>{' '}
                  <dd>
                    <code>{contentPack.id}</code>
                  </dd>
                </span>
              )}
              {contentPack.parameters && !verbose && (
                <span>
                  <dt>{t('details.parameters')}</dt> <dd>{contentPack.parameters.length}</dd>
                </span>
              )}
              {contentPack.entities && !verbose && (
                <span>
                  <dt>{t('details.entities')}</dt> <dd>{contentPack.entities.length}</dd>
                </span>
              )}
            </dl>
          </div>
          {contentPack.description && (
            <div>
              <h2>{t('details.description')}</h2>
              <br />
              <Well>
                <Markdown text={contentPack.description} />
              </Well>
            </div>
          )}
          <br />
          {contentPack.status && <ContentPackStatus states={contentPack.states} />}
          <br />
          <br />
          {contentPack.constraints && showConstraints && (
            <div>
              <ContentPackConstraints constraints={constraints} />
              <br />
            </div>
          )}
          {contentPack.entities && contentPack.entities.length > 0 && verbose && (
            <ContentPackEntitiesList contentPack={contentPack} readOnly />
          )}
          {contentPack.parameters && contentPack.parameters.length > 0 && verbose && (
            <ContentPackParameterList contentPack={contentPack} readOnly />
          )}
        </div>
      </Col>
    </Row>
  );
};

export default ContentPackDetails;
