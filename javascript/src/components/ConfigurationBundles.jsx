/** @jsx React.DOM */

'use strict';

var React = require('react');
var BootstrapAccordion = require('./BootstrapAccordion');
var BootstrapAccordionGroup = require('./BootstrapAccordionGroup');
var SourceType = require('./SourceType');
var ConfigurationBundlePreview = require('./ConfigurationBundlePreview');

var ConfigurationBundles = React.createClass({
    getInitialState: function() {
        return {
            sourceTypeId: "",
            sourceTypeDescription: "",
            bundles: []
        };
    },
    handleSourceTypeChange: function(sourceTypeId, sourceTypeDescription) {
        this.setState({sourceTypeId: sourceTypeId, sourceTypeDescription: sourceTypeDescription});
    },
    componentDidMount: function() {
        $.get('/a/system/bundles', function(result) {
            if (this.isMounted()) {
                this.setState({
                    bundles: result
                });
            }
        }.bind(this));
    },
    _getCategoriesHtml: function() {
        var categories = $.map(this.state.bundles, function( bundles, category){ return category; });
        categories.sort();
        return categories.map(function (category) {
            return this._getSourceTypeHtml(category);
        }, this );
    },
    _getSourceTypeHtml: function(category) {
        var bundles = this._getSortedBundles(category);
        return (
            <BootstrapAccordionGroup key={category} name={category}>
                <ul>
                    {bundles.map(function(bundle){
                        return (
                            <li key={bundle.id}>
                    <SourceType id={bundle.id}
                    name={bundle.name}
                    description={bundle.description}
                    onSelect={this.handleSourceTypeChange}/>
                </li>
            );
            }, this)}
            </ul>
                </BootstrapAccordionGroup>
            );
    },
    _getSortedBundles: function(category) {
        var bundles = this.state.bundles[category];
        bundles.sort(function(bundle1, bundle2){
            if (bundle1.name > bundle2.name)
                return 1;
            if (bundle1.name < bundle2.name)
                return -1;
            return 0;
        });
        return bundles;
    },
    render: function() {
        return (
            <div className="configuration-bundles row-fluid">
                <div className="span6">
                    <BootstrapAccordion>
                            {this._getCategoriesHtml()}
                        <BootstrapAccordionGroup name="Custom">
                            <form method="POST" action="/a/system/bundles" className="form-inline upload" encType="multipart/form-data">
                                <input type="file" name="bundle" />
                                <button type="submit" className="btn btn-success">Upload</button>
                            </form>
                        </BootstrapAccordionGroup>
                    </BootstrapAccordion>
                </div>
                <div className="span6">
                    <ConfigurationBundlePreview sourceTypeId={this.state.sourceTypeId} sourceTypeDescription={this.state.sourceTypeDescription}>
                        <p>Select an item in the right list to preview it.</p>
                    </ConfigurationBundlePreview>
                </div>
            </div>
            );
    }
});

module.exports = ConfigurationBundles;
