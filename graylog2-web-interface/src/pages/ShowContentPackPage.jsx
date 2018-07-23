import React from 'react';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Row, Col, Button, ButtonToolbar } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Spinner from 'components/common/Spinner';

import Routes from 'routing/Routes';

import UserNotification from 'util/UserNotification';
import { DocumentTitle, PageHeader } from 'components/common';
import ContentPackDetails from 'components/content-packs/ContentPackDetails';
import ContentPackVersions from 'components/content-packs/ContentPackVersions';
import ContentPackInstallations from 'components/content-packs/ContentPackInstallations';
import CombinedProvider from 'injection/CombinedProvider';
import ShowContentPackStyle from './ShowContentPackPage.css';

const { ContentPacksActions, ContentPacksStore } = CombinedProvider.get('ContentPacks');

const ShowContentPackPage = createReactClass({
  displayName: 'ShowContentPackPage',

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(ContentPacksStore)],

  getInitialState() {
    return {
      selectedVersion: undefined,
    };
  },


  componentDidMount() {
    ContentPacksActions.get(this.props.params.contentPackId);
    ContentPacksActions.installList(this.props.params.contentPackId);
  },

  _onVersionChanged(newVersion) {
    this.setState({ selectedVersion: newVersion });
  },

  _deleteContentPackRev(contentPackId, revision) {
    if (window.confirm('You are about to delete this content pack, are you sure?')) {
      ContentPacksActions.deleteRev(contentPackId, revision).then(() => {
        UserNotification.success('Content Pack deleted successfully.', 'Success');
        ContentPacksActions.get(contentPackId);
      }, () => {
        UserNotification.error('Deleting content pack failed, please check your logs for more information.', 'Error');
      });
    }
  },

  _uninstallContentPackRev(contentPackId, installId) {
    if (window.confirm('You are about to uninstall this content pack installation, are you sure?')) {
      ContentPacksActions.uninstall(contentPackId, installId).then(() => {
        UserNotification.success('Content Pack uninstalled successfully.', 'Success');
        ContentPacksActions.installList(contentPackId);
      }, () => {
        UserNotification.error('Uninstall content pack failed, please check your logs for more information.', 'Error');
      });
    }
  },

  _getLastVersion() {
    return lodash.last(Object.keys(this.state.contentPack).filter(key => !isNaN(key)).sort());
  },

  render() {
    if (!this.state.contentPack) {
      return (<Spinner />);
    }

    const { contentPack, selectedVersion } = this.state;
    const lastVersion = this._getLastVersion();
    const lastPack = contentPack[lastVersion];
    return (
      <DocumentTitle title="Content packs">
        <span>
          <PageHeader title="Content packs">
            <span>
              Content packs accelerate the set up process for a specific data source. A content pack can include inputs/extractors, streams, and dashboards.
            </span>

            <span>
              Find more content packs in {' '}
              <a href="https://marketplace.graylog.org/" target="_blank" rel="noopener noreferrer">the Graylog Marketplace</a>.
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.LIST}>
                <Button bsStyle="info">Content Packs</Button>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.edit(encodeURIComponent(lastPack.id), encodeURIComponent(lastPack.rev))}>
                <Button bsStyle="primary">Edit</Button>
              </LinkContainer>
            </ButtonToolbar>
          </PageHeader>

          <Row>
            <Col md={4} className="content">
              <div id="content-pack-versions">
                <Row className={ShowContentPackStyle.leftRow}>
                  <Col>
                    <h2>Versions</h2>
                    <ContentPackVersions contentPack={contentPack}
                                         onChange={this._onVersionChanged}
                                         onDeletePack={this._deleteContentPackRev} />
                  </Col>
                </Row>
                <Row className={ShowContentPackStyle.leftRow}>
                  <Col>
                    <h2>Installations</h2>
                    <ContentPackInstallations installations={this.state.installations}
                                              onUninstall={this._uninstallContentPackRev}
                    />
                  </Col>
                </Row>
              </div>
            </Col>
            <Col md={8} className="content">
              <ContentPackDetails contentPack={contentPack[selectedVersion]} showConstraints verbose />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default ShowContentPackPage;
