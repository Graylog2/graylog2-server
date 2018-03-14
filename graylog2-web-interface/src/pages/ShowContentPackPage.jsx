import React from 'react';
import PropTypes from 'prop-types';
import { Row, Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Spinner from 'components/common/Spinner';

import Routes from 'routing/Routes';

import { DocumentTitle, PageHeader } from 'components/common';
import ContentPackDetails from 'components/content-packs/ContentPackDetails';
import ContentPackStores from 'stores/content-packs/ContentPackStores';

class ContentPacksPage extends React.Component {
  static propTypes = {
    params: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);
    this.state = {
      contentPack: undefined,
    };
  }

  componentDidMount() {
    this._loadContentPack();
  }

  _loadContentPack() {
    ContentPackStores.get(this.props.params.contentPackId).then((contentPack) => {
      this.setState({ contentPack: contentPack });
    });
  }

  render() {
    if (!this.state.contentPack) {
      return (<Spinner />);
    }

    const { contentPack } = this.state;
    return (
      <DocumentTitle title="Content packs">
        <span>
          <PageHeader title="Content packs">
            <span>
              Content packs accelerate the set up process for a specific data source. A content pack can include inputs/extractors, streams, and dashboards.
            </span>

            <span>
              Find more content packs in {' '}
              <a href="https://marketplace.graylog.org/" target="_blank">the Graylog Marketplace</a>.
            </span>

            <div>
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.LIST}>
                <Button bsStyle="info" bsSize="large">Content Packs</Button>
              </LinkContainer>
            </div>
          </PageHeader>

          <Row>
            <Col md={6} className="content">
              <div id="content-pack-versions">
                <h2>Versions</h2>
              </div>
            </Col>
            <Col md={6} className="content">
              <ContentPackDetails contentPack={contentPack} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default ContentPacksPage;
