/** @jsx React.DOM */

'use strict';

var React = require('React');
var BootstrapAccordionGroup = require('./BootstrapAccordionGroup');

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
                            <label className="radio">
                                <input type="radio" name="linuxBundles" id="ubuntuSyslog" value="ubuntuSyslog" defaultChecked/>
                                Ubuntu Syslog
                            </label>
                        </li>
                        <li>
                            <label className="radio">
                                <input type="radio" name="linuxBundles" id="redHatSyslog" value="redHatSyslog"/>
                                RedHat Syslog
                            </label>
                        </li>
                    </ul>
                </BootstrapAccordionGroup>
                <BootstrapAccordionGroup name="Switches">
                    <ul>
                        <li>
                            <label className="radio">
                                <input type="radio" name="linuxBundles" id="ciscoCatalyst3560" value="ciscoCatalyst3560"/>
                                Cisco Catalyst 3560
                            </label>
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