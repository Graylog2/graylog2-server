'use strict';

var React = require('react');
var BootstrapAccordion = require('../bootstrap/BootstrapAccordion');
var BootstrapAccordionGroup = require('../bootstrap/BootstrapAccordionGroup');
var SourceType = require('./SourceType');
var ConfigurationBundlePreview = require('./ConfigurationBundlePreview');
var $ = require('jquery'); // excluded and shimed

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
        $.get('/a/system/contentpacks', (result) => {
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
        return categories.map((category) => this._getSourceTypeHtml(category), this);
    },
    _getSourceTypeHtml(category) {
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
            <BootstrapAccordionGroup key={category} name={category}>
                <ul>
                    {bundlesJsx}
                </ul>
            </BootstrapAccordionGroup>
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
            <div className="configuration-bundles row-fluid">
                <div className="span6">
                    <BootstrapAccordion>
                            {this._getCategoriesHtml()}
                        <BootstrapAccordionGroup name="Import content pack">
                            <form method="POST" action="/a/system/contentpacks" className="form-inline upload" encType="multipart/form-data">
                                <span className="help-block">Please apply the content pack after uploading it to make the changes effective.</span>
                                <input type="file" name="bundle" />
                                <button type="submit" className="btn btn-success">Upload</button>
                            </form>
                        </BootstrapAccordionGroup>
                    </BootstrapAccordion>
                </div>
                <div className="span6">
                    <ConfigurationBundlePreview sourceTypeId={this.state.sourceTypeId} sourceTypeDescription={this.state.sourceTypeDescription}/>
                </div>
            </div>
        );
    }
});

module.exports = ConfigurationBundles;
