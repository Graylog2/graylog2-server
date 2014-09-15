/** @jsx React.DOM */

'use strict';

var React = require('react');
var Card = require('./Card');
var BootstrapAccordion = require('./BootstrapAccordion');
var BootstrapAccordionGroup = require('./BootstrapAccordionGroup');
var SourceType = require('./SourceType');
var QuickStartPreview = require('./QuickStartPreview');

var QuickStartCard = React.createClass({
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
        return categories.map(function (category) {
                return this._getSourceTypeHtml(category);
            }, this );
    },
    _getSourceTypeHtml: function(category) {
        var bundles = this.state.bundles[category];
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
    render: function () {
        var quickStartDescription = <p>New to Graylog2? Select one of the preconfigured setups to get you started:</p>;

        return (
            <Card title="Quick Start" icon="icon-plane">
                {quickStartDescription}
                <div className="row">
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
                    <div className="span4 offset1">
                        <QuickStartPreview sourceTypeId={this.state.sourceTypeId} sourceTypeDescription={this.state.sourceTypeDescription}>
                            <p>Select an item in the right list to preview it.</p>
                        </QuickStartPreview>
                    </div>
                </div>
            </Card>
            );
    }
});

module.exports = QuickStartCard;
