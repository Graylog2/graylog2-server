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

import { Alert, Col, Row } from 'components/bootstrap';
import useStreamsByIndexSet from 'components/inputs/InputSetupWizard/hooks/useStreamsByIndexSet';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

type Props = {
  selectedIndexSetId?: string;
  indexSets: Array<IndexSet>;
};

const SelectedIndexSetAlert = ({ selectedIndexSetId = undefined, indexSets }: Props) => {
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
          <Alert title="Default Index Set selected" bsStyle="info">
            You have selected the Default Index Set.
            <br />
            We recommend against this: as the potential recipient of messages of many different formats (any message
            with no routing, via the Default Stream), the Default Index Set is susceptible to reaching the maximum
            per-Index field limit of the search backend if used extensively.
          </Alert>
        </Col>
      </Row>
    );
  }

  if (isAlreadyUsedByStreams()) {
    return (
      <Row>
        <Col md={12}>
          <Alert title="Selected index set already associated with another stream" bsStyle="info">
            The selected index set is already associated with another stream(s).
            <br />
            Note that each Index has a unique field limit, defaulting to 1000.
            <br />
            For this reason, we recommend that streams be organised by log format across multiple Index Sets where
            possible.
          </Alert>
        </Col>
      </Row>
    );
  }

  return null;
};

export default SelectedIndexSetAlert;
