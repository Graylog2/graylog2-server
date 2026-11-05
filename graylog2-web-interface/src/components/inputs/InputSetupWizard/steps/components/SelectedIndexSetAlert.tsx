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
import { useTranslation } from 'react-i18next';

import { Alert, Col, Row } from 'components/bootstrap';
import useStreamsByIndexSet from 'components/inputs/InputSetupWizard/hooks/useStreamsByIndexSet';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

type Props = {
  selectedIndexSetId?: string;
  indexSets: Array<IndexSet>;
};

const StyledAlert = styled(Alert)`
  margin-top: 0;
  margin-bottom: 0;
`;

const SelectedIndexSetAlert = ({ selectedIndexSetId = undefined, indexSets }: Props) => {
  const { t } = useTranslation('inputs');
  const { data: streamsByIndexSetIdData } = useStreamsByIndexSet(selectedIndexSetId, !!selectedIndexSetId);

  const isDefaultIndexSet = () => {
    const indexSet = indexSets?.find(({ id }) => id === selectedIndexSetId);

    return indexSet?.default ?? false;
  };

  const isAlreadyUsedByStreams = () => {
    if (!streamsByIndexSetIdData) return false;

    return streamsByIndexSetIdData.total > 0;
  };

  if (!selectedIndexSetId) return null;

  if (isDefaultIndexSet()) {
    return (
      <Row>
        <Col md={12}>
          <StyledAlert title={t('setup_wizard.index_set_alert.default_title')} bsStyle="info">
            {t('setup_wizard.index_set_alert.default_selected')}
            <br />
            {t('setup_wizard.index_set_alert.default_recommendation')}
          </StyledAlert>
        </Col>
      </Row>
    );
  }

  if (isAlreadyUsedByStreams()) {
    return (
      <Row>
        <Col md={12}>
          <Alert title={t('setup_wizard.index_set_alert.in_use_title')} bsStyle="info">
            {t('setup_wizard.index_set_alert.in_use')}
            <br />
            {t('setup_wizard.index_set_alert.field_limit')}
            <br />
            {t('setup_wizard.index_set_alert.organise_recommendation')}
          </Alert>
        </Col>
      </Row>
    );
  }

  return null;
};

export default SelectedIndexSetAlert;
