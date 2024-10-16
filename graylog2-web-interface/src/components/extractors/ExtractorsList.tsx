import * as React from 'react';
import { useEffect, useState } from 'react';

import { Row, Col, Button } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';
import AddExtractorWizard from 'components/extractors/AddExtractorWizard';
import EntityList from 'components/common/EntityList';
import { ExtractorsActions, ExtractorsStore } from 'stores/extractors/ExtractorsStore';
import type { ExtractorType, InputSummary, NodeSummary } from 'stores/extractors/ExtractorsStore';
import { useStore } from 'stores/connect';

import ExtractorsListItem from './ExtractorsListItem';
import ExtractorsSortModal from './ExtractorSortModal';

type Props = {
  input: InputSummary,
  node: NodeSummary,
};

const fetchExtractors = (inputId: string) => {
  ExtractorsActions.list(inputId);
};

const ExtractorsList = ({ input, node }: Props) => {
  const [showSortModal, setShowSortModal] = useState(false);
  const extractors = useStore(ExtractorsStore, (state) => state.extractors);

  useEffect(() => {
    fetchExtractors(input.id);
  }, [input.id]);

  const _formatExtractor = (extractor: ExtractorType) => (
    <ExtractorsListItem key={extractor.id}
                        extractor={extractor}
                        inputId={input.id}
                        nodeId={node.node_id} />
  );

  const _isLoading = () => !extractors;

  const _openSortModal = () => {
    setShowSortModal(true);
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
      {showSortModal && (
        <ExtractorsSortModal input={input}
                             extractors={extractors}
                             onClose={() => setShowSortModal(false)}
                             onSort={() => fetchExtractors(input.id)} />
      )}
    </div>
  );
};

export default ExtractorsList;
