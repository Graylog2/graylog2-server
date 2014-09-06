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
        return {sourceType: ""};
    },
    handleSourceTypeChange: function(sourceType) {
        this.setState({sourceType: sourceType});
    },
    render: function () {
        var quickStartDescription = <p>New to Graylog2? Select one of the preconfigured setups to get you started:</p>;

        return (
            <Card title="Quick Start" icon="icon-plane">
                    {quickStartDescription}
                <div className="row">
                    <div className="span6">
                        <BootstrapAccordion>
                            <BootstrapAccordionGroup name="Firewalls">
                                <p>Nothing to see here!</p>
                            </BootstrapAccordionGroup>
                            <BootstrapAccordionGroup name="Linux">
                                <ul>
                                    <li>
                                        <SourceType
                                            name="ubuntuSyslog"
                                            description="Ubuntu Syslog"
                                            onSelect={this.handleSourceTypeChange}/>
                                    </li>
                                    <li>
                                        <SourceType
                                            name="redHatSyslog"
                                            description="RedHat Syslog"
                                            onSelect={this.handleSourceTypeChange}/>
                                    </li>
                                </ul>
                            </BootstrapAccordionGroup>
                            <BootstrapAccordionGroup name="Switches">
                                <ul>
                                    <li>
                                        <SourceType
                                            name="ciscoCatalyst3560"
                                            description="Cisco Catalyst 3560"
                                            onSelect={this.handleSourceTypeChange}/>
                                    </li>
                                </ul>
                            </BootstrapAccordionGroup>
                            <BootstrapAccordionGroup name="Windows">
                                <p>Nothing to see here!</p>
                            </BootstrapAccordionGroup>
                            <BootstrapAccordionGroup name="More">
                                <p>Upload your own bundle</p>
                            </BootstrapAccordionGroup>
                        </BootstrapAccordion>
                    </div>
                    <div className="span4 offset1">
                        <QuickStartPreview sourceType={this.state.sourceType}>
                            <p>Select an item in the right list to preview it.</p>
                        </QuickStartPreview>
                    </div>
                </div>
            </Card>
            );
    }
});

module.exports = QuickStartCard;
