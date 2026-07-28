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

import { Alert, Modal, Table } from 'components/bootstrap';
import { Spinner } from 'components/common';
import useIncompatibleIndices from 'components/indices/hooks/useIncompatibleIndices';

type Props = {
  show: boolean;
  onClose: () => void;
};

const IncompatibleIndicesModal = ({ show, onClose }: Props) => {
  const { data: incompatibleIndices, isError, isLoading } = useIncompatibleIndices();

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header>
        <Modal.Title>Index Versions</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {isLoading && <Spinner />}

        {!isLoading && isError && <Alert bsStyle="danger">Could not load incompatible indices.</Alert>}

        {!isLoading && !isError && incompatibleIndices.length > 0 && (
          <>
            <Alert bsStyle="info">
              Found <strong>{incompatibleIndices.length}</strong>{' '}
              {incompatibleIndices.length === 1 ? 'index' : 'indices'} that were created with an incompatible, previous
              major version of OpenSearch. These indices may need to be re-indexed for compatibility with future
              OpenSearch major versions.
            </Alert>
            <Table condensed>
              <thead>
                <tr>
                  <th>Index</th>
                  <th>Version</th>
                  <th>Graylog-managed</th>
                  <th>Warm</th>
                </tr>
              </thead>
              <tbody>
                {incompatibleIndices.map(({ index_name, version, warm_index, managed_index }) => (
                  <tr key={index_name}>
                    <td>{index_name}</td>
                    <td>{version}</td>
                    <td>{managed_index ? 'Yes' : 'No'}</td>
                    <td>{warm_index ? 'Yes' : 'No'}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </>
        )}

        {!isLoading && !isError && incompatibleIndices.length === 0 && (
          <Alert bsStyle="success">
            All indices are up to date. No indices created with incompatible, previous major versions of OpenSearch were
            found.
          </Alert>
        )}
      </Modal.Body>
    </Modal>
  );
};

export default IncompatibleIndicesModal;
