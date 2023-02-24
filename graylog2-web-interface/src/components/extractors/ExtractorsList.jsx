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
import PropTypes from 'prop-types';
import * as React from 'react';
import { useEffect, useRef, useState } from 'react';

import { Row, Col, Button } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';
import AddExtractorWizard from 'components/extractors/AddExtractorWizard';
import EntityList from 'components/common/EntityList';
import { ExtractorsActions } from 'stores/extractors/ExtractorsStore';

import ExtractorsListItem from './ExtractorsListItem';
import ExtractorsSortModal from './ExtractorSortModal';

const fetchExtractors = (inputId, callback) => {
  ExtractorsActions.list.triggerPromise(inputId).then((data) => callback(data?.extractors));
};

const ExtractorsList = ({ input, node }) => {
  const [extractors, setExtractors] = useState(null);
  const sortModal = useRef(null);

  useEffect(() => {
    fetchExtractors(input.id, setExtractors);
  }, [input.id]);

  const getNodeId = (_node) => {
    if (Array.isArray(_node)) {
      return _node[0]?.node_id || '';
    }

    return _node?.node_id || '';
  };

  const _formatExtractor = (extractor) => {
    return (
      <ExtractorsListItem key={extractor.id}
                          extractor={extractor}
                          inputId={input.id}
                          nodeId={getNodeId(node)} />
    );
  };

  const _isLoading = () => {
    return !extractors;
  };

  const _openSortModal = () => {
    sortModal.current?.open();
  };

  let sortExtractorsButton;

  if (extractors?.length > 1) {
    sortExtractorsButton = (
      <Button bsSize="xsmall" bsStyle="primary" className="pull-right" onClick={_openSortModal}>
        Sort extractors
      </Button>
    );
  }

  const formattedExtractors = extractors
    ?.sort((extractor1, extractor2) => extractor1.order - extractor2.order)
    .map(_formatExtractor);

  if (_isLoading()) {
    return <Spinner />;
  }

  return (
    <div>
      <AddExtractorWizard inputId={input.id} />
      <Row className="content extractor-list">
        <Col md={12}>
          <Row className="row-sm">
            <Col md={8}>
              <h2>Configured extractors</h2>
            </Col>
            <Col md={4}>
              {sortExtractorsButton}
            </Col>
          </Row>
          <EntityList bsNoItemsStyle="info"
                      noItemsText="This input has no configured extractors."
                      items={formattedExtractors} />
        </Col>
      </Row>
      <ExtractorsSortModal ref={(modal) => { sortModal.current = modal; }}
                           input={input}
                           extractors={extractors}
                           onSort={() => fetchExtractors(input.id, setExtractors)} />
    </div>
  );
};

ExtractorsList.propTypes = {
  input: PropTypes.object.isRequired,
  node: PropTypes.oneOfType([PropTypes.object, PropTypes.array]).isRequired,
};

export default ExtractorsList;
