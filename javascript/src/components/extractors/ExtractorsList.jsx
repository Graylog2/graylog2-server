import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import {Row, Col} from 'react-bootstrap';

import Spinner from 'components/common/Spinner';
import AddExtractor from 'components/extractors/AddExtractor';
import EntityList from 'components/common/EntityList';
import ExtractorsListItem from './ExtractorsListItem';

import ExtractorsActions from 'actions/extractors/ExtractorsActions';
import ExtractorsStore from 'stores/extractors/ExtractorsStore';

const ExtractorsList = React.createClass({
  propTypes: {
    input: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(ExtractorsStore), Reflux.ListenerMethods],
  componentDidMount() {
    ExtractorsActions.list.triggerPromise(this.props.input.input_id);
  },
  _formatExtractor(extractor) {
    return <ExtractorsListItem key={extractor.id} extractor={extractor}/>;
  },
  _isLoading() {
    return !this.state.extractors;
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    return (
      <div>
        <AddExtractor inputId={this.props.input.input_id}/>
        <Row className="content extractor-list">
          <Col md={12}>
            <h2>Configured extractors</h2>
            <EntityList bsNoItemsStyle="info" noItemsText="This input has no configured extractors."
                        items={this.state.extractors.map(this._formatExtractor)} />
          </Col>
        </Row>
      </div>
    );
  },
});

export default ExtractorsList;
