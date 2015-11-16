import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import {Row, Col} from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';

import Spinner from 'components/common/Spinner';
import AddExtractorWizard from 'components/extractors/AddExtractorWizard';
import EntityList from 'components/common/EntityList';
import ExtractorsListItem from './ExtractorsListItem';

import ExtractorsActions from 'actions/extractors/ExtractorsActions';
import ExtractorsStore from 'stores/extractors/ExtractorsStore';

const ExtractorsList = React.createClass({
  propTypes: {
    input: PropTypes.object.isRequired,
    node: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(ExtractorsStore), Reflux.ListenerMethods],
  componentDidMount() {
    ExtractorsActions.list.triggerPromise(this.props.input.input_id);
  },
  _formatExtractor(extractor) {
    return <ExtractorsListItem key={extractor.id} extractor={extractor} inputId={this.props.input.input_id} nodeId={this.props.node.node_id}/>;
  },
  _isLoading() {
    return !this.state.extractors;
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    const formattedExtractors = this.state.extractors
      .sort((extractor1, extractor2) => naturalSort(extractor1.order, extractor2.order))
      .map(this._formatExtractor);

    return (
      <div>
        <AddExtractorWizard inputId={this.props.input.input_id}/>
        <Row className="content extractor-list">
          <Col md={12}>
            <h2>Configured extractors</h2>
            <EntityList bsNoItemsStyle="info" noItemsText="This input has no configured extractors."
                        items={formattedExtractors}/>
          </Col>
        </Row>
      </div>
    );
  },
});

export default ExtractorsList;
