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

import { Alert, Modal } from 'components/bootstrap';
import { Spinner } from 'components/common';
import useOutdatedIndices from 'components/indices/hooks/useOutdatedIndices';

type Props = {
  show: boolean;
  onClose: () => void;
};

const OutdatedIndicesModal = ({ show, onClose }: Props) => {
  const { data: outdatedIndices, isError, isLoading } = useOutdatedIndices();

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header>
        <Modal.Title>Outdated Indices</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {isLoading && <Spinner />}

        {!isLoading && isError && (
          <Alert bsStyle="danger">
            Could not load outdated indices.
          </Alert>
        )}

        {!isLoading && !isError && outdatedIndices.length > 0 && (
          <>
            <Alert bsStyle="info">
              Found <strong>{outdatedIndices.length}</strong> {outdatedIndices.length === 1 ? 'index' : 'indices'} that
              were created with an outdated, previous major version of OpenSearch. These indices may need to be
              re-indexed for compatibility with future OpenSearch major versions.
            </Alert>
            <ul>
              {outdatedIndices.map((index) => (
                <li key={index}>{index}</li>
              ))}
            </ul>
          </>
        )}

        {!isLoading && !isError && outdatedIndices.length === 0 && (
          <Alert bsStyle="success">
            All indices are up to date. No indices created with outdated, previous major versions of OpenSearch were
            found.
          </Alert>
        )}
      </Modal.Body>
    </Modal>
  );
};

export default OutdatedIndicesModal;
