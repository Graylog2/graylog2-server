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
import { Trans, useTranslation } from 'react-i18next';

import { Row, Col } from 'components/bootstrap';
import { Link, DocumentTitle, PageHeader, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import withParams from 'routing/withParams';
import { NodesStore } from 'stores/nodes/NodesStore';
import useParams from 'routing/useParams';
import { useStore } from 'stores/connect';
import useProductName from 'brand-customization/useProductName';
import { InputsOverview } from 'components/inputs/InputsOveriew';
import useInputTypes from 'hooks/useInputTypes';
import useInputTypesDescriptions from 'hooks/useInputTypesDescriptions';

const NodeInputsPage = () => {
  const { t } = useTranslation('inputs');
  const productName = useProductName();
  const { nodeId } = useParams();
  const { data: inputTypes, isLoading: isLoadingInputTypes } = useInputTypes();
  const { data: inputTypeDescriptions, isLoading: isLoadingInputTypesDescriptions } = useInputTypesDescriptions();

  const { nodes } = useStore(NodesStore);
  const node = nodes?.[nodeId];

  if (!node || isLoadingInputTypes || isLoadingInputTypesDescriptions) {
    return <Spinner />;
  }

  const title = t('pages.node_inputs_title', { nodeId: node.short_node_id, hostname: node.hostname });

  return (
    <DocumentTitle title={title}>
      <div>
        <PageHeader title={title}>
          <span>
            {t('pages.node_inputs_description', { productName })}
            <br />
            <Trans
              t={t}
              i18nKey="pages.node_inputs_hint"
              components={{ inputsLink: <Link to={Routes.SYSTEM.INPUTS} /> }}
            />
          </span>
        </PageHeader>
        <Row className="content">
          <Col md={12}>
            <h2>{t('pages.local_inputs')}</h2>
            <InputsOverview
              node={node}
              inputTypes={inputTypes}
              inputTypeDescriptions={inputTypeDescriptions}
              entityTableId="node-inputs"
            />
          </Col>
        </Row>
        <Row className="content">
          <Col md={12}>
            <h2>{t('pages.global_inputs')}</h2>
            <InputsOverview
              global={true}
              inputTypes={inputTypes}
              inputTypeDescriptions={inputTypeDescriptions}
              entityTableId="global-inputs"
              withoutURLParams
            />
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default withParams(NodeInputsPage);
