/** @jsx React.DOM */

'use strict';

var React = require('react');
var BootstrapAccordion = require('../bootstrap/BootstrapAccordion');
var BootstrapAccordionGroup = require('../bootstrap/BootstrapAccordionGroup');
var SourceType = require('./SourceType');
var ConfigurationBundlePreview = require('./ConfigurationBundlePreview');
var $ = require('jquery'); // excluded and shimed

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
        $.get('/a/system/contentpacks', function(result) {
            if (this.isMounted()) {
                this.setState({
                    bundles: result
                });
            }
        }.bind(this));
    },
    _getCategoriesHtml: function() {
        // TODO: the mocking framework will mock the $.map function, replace with foreach.
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
    render: function() {
        return (
            <div className="configuration-bundles row-fluid">
                <div className="span6">
                    <BootstrapAccordion>
                            {this._getCategoriesHtml()}
                        <BootstrapAccordionGroup name="Import">
                            <form method="POST" action="/a/system/contentpacks" className="form-inline upload" encType="multipart/form-data">
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
