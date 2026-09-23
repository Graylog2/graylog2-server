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

import ProductName from 'brand-customization/ProductName';
import { Alert, Button, Modal } from 'components/bootstrap';
import { ExternalLink } from 'components/common';
import useRunsWithDataNode from 'components/datanode/hooks/useRunsWithDataNode';
import useIncompatibleIndices from 'components/indices/hooks/useIncompatibleIndices';
import DataNodeMigrationLink from 'components/indices/incompatible-indices/DataNodeMigrationLink';
import IncompatibleIndicesTable from 'components/indices/incompatible-indices/IncompatibleIndicesTable';
import useSearchVersionCheck from 'hooks/useSearchVersionCheck';
import DocsHelper from 'util/DocsHelper';

type Props = {
  show: boolean;
  onClose: () => void;
};

const IncompatibleIndicesModal = ({ show, onClose }: Props) => {
  const { data: runsWithDataNode } = useRunsWithDataNode();
  const { data: incompatibleIndices } = useIncompatibleIndices();
  const { data: openSearchVersionCheck } = useSearchVersionCheck('opensearch');
  const showUnsupportedUpgradeWarning =
    runsWithDataNode === false && openSearchVersionCheck?.satisfied === true && incompatibleIndices.length > 0;

  return (
    <Modal show={show} onHide={onClose} bsSize="xl">
      <Modal.Header>
        <Modal.Title>Index Versions</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {showUnsupportedUpgradeWarning && (
          <Alert bsStyle="warning">
            <p>
              The presence of incompatible indices prevents this cluster from upgrading to OpenSearch 3.x at this time -
              doing so would be a destructive action.
            </p>
            <p>
              The recommended upgrade path is to <DataNodeMigrationLink text="migrate from OpenSearch to Data Node" />{' '}
              using the provided wizard (
              <ExternalLink href={DocsHelper.toString(DocsHelper.PAGES.DATA_NODE_MIGRATION)}>
                documentation
                <span className="sr-only"> (opens in a new tab)</span>
              </ExternalLink>
              ), and then use the built-in Data Node upgrade automation to proceed.
            </p>
            <p>
              <ProductName /> does not currently support the direct OpenSearch 2.x to 3.x upgrade process where
              incompatible indices are present, due to the risks involved. A subsequent release will add support for
              this path, or if it is urgent, please reach out to <ProductName /> to discuss a Professional Services
              engagement.
            </p>
            <p>OpenSearch 2.x remains in support until November 2027.</p>
          </Alert>
        )}
        <Alert bsStyle="info">
          Any indices created with an incompatible, previous major version of OpenSearch need to be archived, deleted or
          reindexed before the search backend can be upgraded to the next major version.
        </Alert>
        <IncompatibleIndicesTable withoutURLParams />
      </Modal.Body>
      <Modal.Footer>
        <Button onClick={onClose}>Close</Button>
      </Modal.Footer>
    </Modal>
  );
};

export default IncompatibleIndicesModal;
