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
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Button, Col, Row } from 'components/bootstrap';
import { Card } from 'components/common';
import type { IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';
import { prepareDataTieringInitialValues, DataTieringVisualisation } from 'components/indices/data-tiering';

type Props = {
  template: IndexSetTemplate,
  handleCardClick: (template: IndexSetTemplate) => void,
  isSelected: boolean
}

const ButtonWrapper = styled.div`
  display: flex;
  justify-content: end;
`;

const IndexSetTemplateCard = ({ template, handleCardClick, isSelected }: Props) => {
  const dataTieringConfig = prepareDataTieringInitialValues(template.index_set_config.data_tiering, PluginStore);

  return (
    <Card>
      <h3>{template.title}</h3>
      {template.index_set_config.use_legacy_rotation && (<p>{template.description}</p>)}
      {!template.index_set_config.use_legacy_rotation && (
        <Row>
          <Col md={8}>
            <DataTieringVisualisation minDays={dataTieringConfig.index_lifetime_min}
                                      maxDays={dataTieringConfig.index_lifetime_max}
                                      minDaysInHot={dataTieringConfig.index_hot_lifetime_min}
                                      warmTierEnabled={dataTieringConfig.warm_tier_enabled}
                                      archiveData={dataTieringConfig.archive_before_deletion} />
          </Col>
          <Col md={4}>{template.description}</Col>
        </Row>
      )}
      <ButtonWrapper>
        <Button onClick={() => handleCardClick(template)} disabled={isSelected}>{isSelected ? 'Selected' : 'Select'}</Button>
      </ButtonWrapper>
    </Card>
  );
};

export default IndexSetTemplateCard;
