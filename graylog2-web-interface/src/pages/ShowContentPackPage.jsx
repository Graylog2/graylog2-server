import React from 'react';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { Row, Col, Button, ButtonToolbar } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Spinner from 'components/common/Spinner';
import { BootstrapModalConfirm } from 'components/bootstrap';

import history from 'util/History';
import Routes from 'routing/Routes';

import UserNotification from 'util/UserNotification';
import { DocumentTitle, PageHeader } from 'components/common';
import ContentPackDetails from 'components/content-packs/ContentPackDetails';
import ContentPackVersions from 'components/content-packs/ContentPackVersions';
import ContentPackInstallations from 'components/content-packs/ContentPackInstallations';
import ContentPackInstallEntityList from 'components/content-packs/ContentPackInstallEntityList';
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
      uninstallEntities: undefined,
      uninstallContentPackId: undefined,
      uninstallInstallId: undefined,
    };
  },


  componentDidMount() {
    ContentPacksActions.get(this.props.params.contentPackId).catch((error) => {
      if (error.status === 404) {
        UserNotification.error(
          `Cannot find Content Pack with the id ${this.props.params.contentPackId} and may have been deleted.`);
      } else {
        UserNotification.error('An internal server error occurred. Please check your logfiles for more information');
      }
      history.push(Routes.SYSTEM.CONTENTPACKS.LIST);
    });
    ContentPacksActions.installList(this.props.params.contentPackId);
  },

  _onVersionChanged(newVersion) {
    this.setState({ selectedVersion: newVersion });
  },

  _deleteContentPackRev(contentPackId, revision) {
    /* eslint-disable-next-line no-alert */
    if (window.confirm('You are about to delete this content pack revision, are you sure?')) {
      ContentPacksActions.deleteRev(contentPackId, revision).then(() => {
        UserNotification.success('Content pack revision deleted successfully.', 'Success');
        ContentPacksActions.get(contentPackId).catch((error) => {
          if (error.status !== 404) {
            UserNotification.error('An internal server error occurred. Please check your logfiles for more information');
          }
          history.push(Routes.SYSTEM.CONTENTPACKS.LIST);
        });
      }, (error) => {
        let errMessage = error.message;
        if (error.responseMessage) {
          errMessage = error.responseMessage;
        }
        UserNotification.error(`Deleting content pack failed: ${errMessage}`, 'Error');
      });
    }
  },

  _onUninstallContentPackRev(contentPackId, installId) {
    ContentPacksActions.uninstallDetails(contentPackId, installId).then((result) => {
      this.setState({ uninstallEntities: result.entities });
    });
    this.setState({
      uninstallContentPackId: contentPackId,
      uninstallInstallId: installId,
    });
    this.modal.open();
  },

  _clearUninstall() {
    this.setState({
      uninstallContentPackId: undefined,
      uninstallInstallId: undefined,
      uninstallEntities: undefined,
    });
    this.modal.close();
  },

  _uninstallContentPackRev() {
    const contentPackId = this.state.uninstallContentPackId;
    ContentPacksActions.uninstall(this.state.uninstallContentPackId, this.state.uninstallInstallId).then(() => {
      UserNotification.success('Content Pack uninstalled successfully.', 'Success');
      ContentPacksActions.installList(contentPackId);
      this._clearUninstall();
    }, () => {
      UserNotification.error('Uninstall content pack failed, please check your logs for more information.', 'Error');
    });
  },

  _installContentPack(contentPackId, contentPackRev, parameters) {
    ContentPacksActions.install(contentPackId, contentPackRev, parameters).then(() => {
      UserNotification.success('Content Pack installed successfully.', 'Success');
      ContentPacksActions.installList(contentPackId);
    }, (error) => {
      UserNotification.error(`Installing content pack failed with status: ${error}.
         Could not install content pack with ID: ${contentPackId}`);
    });
  },

  render() {
    if (!this.state.contentPackRevisions) {
      return (<Spinner />);
    }

    const { contentPackRevisions, selectedVersion, constraints } = this.state;
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
            </ButtonToolbar>
          </PageHeader>

          <Row>
            <Col md={4} className="content">
              <div id="content-pack-versions">
                <Row className={ShowContentPackStyle.leftRow}>
                  <Col>
                    <h2>Versions</h2>
                    <ContentPackVersions contentPackRevisions={contentPackRevisions}
                                         onInstall={this._installContentPack}
                                         onChange={this._onVersionChanged}
                                         onDeletePack={this._deleteContentPackRev} />
                  </Col>
                </Row>
                <Row className={ShowContentPackStyle.leftRow}>
                  <Col>
                    <h2>Installations</h2>
                    <ContentPackInstallations installations={this.state.installations}
                                              onUninstall={this._onUninstallContentPackRev}
                    />
                  </Col>
                </Row>
              </div>
            </Col>
            <Col md={8} className="content">
              <ContentPackDetails contentPack={contentPackRevisions.contentPack(selectedVersion)}
                                  constraints={constraints[selectedVersion]}
                                  showConstraints
                                  verbose />
            </Col>
          </Row>
        </span>
        <BootstrapModalConfirm
          ref={(c) => { this.modal = c; }}
          title="Do you really want to uninstall this Content Pack?"
          onConfirm={this._uninstallContentPackRev}
          onCancel={this._clearUninstall}
        >
          <ContentPackInstallEntityList uninstall entities={this.state.uninstallEntities} />
        </BootstrapModalConfirm>
      </DocumentTitle>
    );
  },
});

export default ShowContentPackPage;
