import React from 'react';
import Reflux from 'reflux';
import { Accordion, Panel, Row, Col } from 'react-bootstrap';
import $ from 'jquery';

import UserNotification from 'util/UserNotification';

import ConfigurationBundlesActions from 'actions/configuration-bundles/ConfigurationBundlesActions';
import ConfigurationBundlesStore from 'stores/configuration-bundles/ConfigurationBundlesStore';

import SourceType from './SourceType';
import ConfigurationBundlePreview from './ConfigurationBundlePreview';
import Spinner from 'components/common/Spinner';

const ConfigurationBundles = React.createClass({
  mixins: [Reflux.connect(ConfigurationBundlesStore)],

  getInitialState() {
    return {
      sourceTypeId: '',
      sourceTypeDescription: '',
    };
  },
  componentDidMount() {
    ConfigurationBundlesActions.list();
  },
  _getCategoriesHtml() {
    const categories = $.map(this.state.configurationBundles, (bundles, category) => category);
    categories.sort();
    return categories.map((category, idx) => this._getSourceTypeHtml(category, idx), this);
  },
  _getSourceTypeHtml(category, idx) {
    const bundles = this._getSortedBundles(category);
    const bundlesJsx = bundles.map((bundle) => {
      return (
        <li key={bundle.id}>
          <SourceType id={bundle.id}
                      name={bundle.name}
                      description={bundle.description}
                      onSelect={this.handleSourceTypeChange}/>
        </li>
      );
    }, this);

    return (
      <Panel key={category} header={category} eventKey={idx}>
        <ul>
          {bundlesJsx}
        </ul>
      </Panel>
    );
  },
  _getSortedBundles(category) {
    const bundles = this.state.configurationBundles[category];
    bundles.sort((bundle1, bundle2) => {
      if (bundle1.name > bundle2.name) {
        return 1;
      }
      if (bundle1.name < bundle2.name) {
        return -1;
      }
      return 0;
    });
    return bundles;
  },
  onSubmit(submitEvent) {
    submitEvent.preventDefault();
    if (!this.refs.uploadedFile.files || !this.refs.uploadedFile.files[0]) {
      return;
    }

    const reader = new FileReader();

    reader.onload = (evt) => {
      const request = evt.target.result;
      ConfigurationBundlesActions.create.triggerPromise(request)
        .then(() => {
          UserNotification.success('Bundle added successfully', 'Success!');
          ConfigurationBundlesActions.list();
        });
    };

    reader.readAsText(this.refs.uploadedFile.files[0]);
  },
  handleSourceTypeChange(sourceTypeId, sourceTypeDescription) {
    this.setState({sourceTypeId: sourceTypeId, sourceTypeDescription: sourceTypeDescription});
  },
  render() {
    return (
      <Row className="configuration-bundles">
        <Col md={6}>
          {this.state.configurationBundles ?
          <Accordion>
            {this._getCategoriesHtml()}
            <Panel header="Import content pack" eventKey={-1}>
              <form onSubmit={this.onSubmit} className="form-inline upload" encType="multipart/form-data">
                <span className="help-block">Please apply the content pack after uploading it to make the changes effective.</span>
                <input ref="uploadedFile" type="file" name="bundle"/>
                <button type="submit" className="btn btn-success">Upload</button>
              </form>
            </Panel>
          </Accordion>
            :
          <Spinner />
            }
        </Col>
        <Col md={6}>
          <ConfigurationBundlePreview sourceTypeId={this.state.sourceTypeId} sourceTypeDescription={this.state.sourceTypeDescription}/>
        </Col>
      </Row>
    );
  },
});

export default ConfigurationBundles;
