/** @jsx React.DOM */

'use strict';

var React = require('React');
var BootstrapAccordionGroup = require('./BootstrapAccordionGroup');
var SourceType = require('./SourceType');

var BootstrapAccordion = React.createClass({
    render: function () {
        return (
            <div id="bundles" className="accordion">
                <BootstrapAccordionGroup name="Firewalls">
                    <p>Nothing to see here!</p>
                </BootstrapAccordionGroup>
                <BootstrapAccordionGroup name="Linux">
                    <ul>
                        <li>
                            <SourceType name="ubuntuSyslog" description="Ubuntu Syslog"/>
                        </li>
                        <li>
                            <SourceType name="redHatSyslog" description="RedHat Syslog"/>
                        </li>
                    </ul>
                </BootstrapAccordionGroup>
                <BootstrapAccordionGroup name="Switches">
                    <ul>
                        <li>
                            <SourceType name="ciscoCatalyst3560" description="Cisco Catalyst 3560"/>
                        </li>
                    </ul>
                </BootstrapAccordionGroup>
                <BootstrapAccordionGroup name="Windows">
                    <p>Nothing to see here!</p>
                </BootstrapAccordionGroup>
                <BootstrapAccordionGroup name="More">
                    <p>Upload your own bundle</p>
                </BootstrapAccordionGroup>
            </div>
        );
    }
});

module.exports = BootstrapAccordion;