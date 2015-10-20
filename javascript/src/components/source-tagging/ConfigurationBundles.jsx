import React from 'react';
import { Accordion, Panel } from 'react-bootstrap';
import SourceType from './SourceType';
import ConfigurationBundlePreview from './ConfigurationBundlePreview';
import URLUtils from "../../util/URLUtils";
import $ from 'jquery';

const fetch = require('logic/rest/FetchProvider').default;
import jsRoutes from 'routing/jsRoutes';

var ConfigurationBundles = React.createClass({
    getInitialState() {
        return {
            sourceTypeId: "",
            sourceTypeDescription: "",
            bundles: []
        };
    },
    handleSourceTypeChange(sourceTypeId, sourceTypeDescription) {
        this.setState({sourceTypeId: sourceTypeId, sourceTypeDescription: sourceTypeDescription});
    },
    // TODO: next time we touch this, we should create a store for this and preprocess the data
    componentDidMount() {
        fetch('GET', URLUtils.qualifyUrl(jsRoutes.controllers.api.BundlesApiController.list().url))
          .then((result) => {
            if (this.isMounted()) {
                this.setState({
                    bundles: result
                });
            }
        });
    },
    _getCategoriesHtml() {
        // TODO: the mocking framework will mock the $.map function, replace with foreach.
        var categories = $.map(this.state.bundles, (bundles, category) => category);
        categories.sort();
        return categories.map((category, idx) => this._getSourceTypeHtml(category, idx), this);
    },
    _getSourceTypeHtml(category, idx) {
      var bundles = this._getSortedBundles(category);
      var bundlesJsx = bundles.map((bundle) => {
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
        var bundles = this.state.bundles[category];
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
    render() {
        return (
            <div className="configuration-bundles row">
                <div className="col-md-6">
                    <Accordion>
                        {this._getCategoriesHtml()}
                        <Panel header="Import content pack" eventKey={-1}>
                            <form method="POST" action={URLUtils.appPrefixed('/a/system/contentpacks')} className="form-inline upload" encType="multipart/form-data">
                                <span className="help-block">Please apply the content pack after uploading it to make the changes effective.</span>
                                <input type="file" name="bundle" />
                                <button type="submit" className="btn btn-success">Upload</button>
                            </form>
                        </Panel>
                    </Accordion>
                </div>
                <div className="col-md-6">
                    <ConfigurationBundlePreview sourceTypeId={this.state.sourceTypeId} sourceTypeDescription={this.state.sourceTypeDescription}/>
                </div>
            </div>
        );
    }
});

module.exports = ConfigurationBundles;
