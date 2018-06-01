import PropTypes from 'prop-types';
import lodash from 'lodash';
import React from 'react';
import { Col, MenuItem, Nav, Navbar, NavDropdown, Panel, Row, Tab, Tabs } from 'react-bootstrap';

import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import FilebeatHelper from './FilebeatHelper';
import TemplatesHelper from './TemplatesHelper';
import IncludesHelper from './IncludesHelper';
import ConfigurationHelperStyle from './ConfigurationHelper.css';

const ConfigurationHelper = React.createClass({
  propTypes: {
    type: PropTypes.string.isRequired,
  },

  getInitialState() {
    return {
      section: undefined,
      paragraph: undefined,
    };
  },

  _onSelect(event) {
    const newState = event.split('.');
    this.setState({ section: newState[0], paragraph: newState[1] });
  },

  _getId(idName, index) {
    const idIndex = index !== undefined ? `. ${index}` : '';
    return idName + idIndex;
  },

  _getEventKey(a, b) {
    return (`${a}.${b}`);
  },

  navDropDowns(content) {
    const dropDowns = [];
    Object.keys(content).forEach((section) => {
      if (!Object.prototype.hasOwnProperty.call(content, section)) {
        return undefined;
      }

      const paragraphs = content[section];
      const menuItems = [];

      for (let i = 0; i < paragraphs.length; i += 1) {
        menuItems.push(
          <MenuItem key={this._getId(section,i)} eventKey={this._getEventKey(section, paragraphs[i])}>{lodash.capitalize(paragraphs[i])}</MenuItem>,
        );
      }
      dropDowns.push(
        <NavDropdown key={this._getId(section)} title={lodash.capitalize(section)} id="basic-nav-dropdown">
          {menuItems}
        </NavDropdown>,
      );
    });

    return dropDowns;
  },

  render() {
    return (
      <Panel header="Filebeat quick reference">
        <Row className="row-sm">
          <Col md={12}>
            <p className={ConfigurationHelperStyle.marginQuickReferenceText}>
              Read the <DocumentationLink page={DocsHelper.PAGES.PIPELINE_RULES}
                                          text="full documentation" />{' '}
              to gain a better understanding of how the Filebeat collector work.
            </p>
          </Col>
        </Row>

        <Row className="row-sm">
          <Col md={12}>
            <Tabs id="configurationsHelper" defaultActiveKey={1} animation={false}>
              <Tab eventKey={1} title="Reference">
                <br />
                <Navbar collapseOnSelect>
                  <Navbar.Collapse>
                    <Nav onSelect={this._onSelect}>
                      {this.navDropDowns(FilebeatHelper.toc)}
                    </Nav>
                  </Navbar.Collapse>
                </Navbar>
                <Panel>
                  <FilebeatHelper section={this.state.section} paragraph={this.state.paragraph} />
                </Panel>
              </Tab>
              <Tab eventKey={2} title="Templates">
                <br />
                <TemplatesHelper />
              </Tab>
              <Tab eventKey={3} title="Includes">
                <br />
                <IncludesHelper />
              </Tab>
            </Tabs>
          </Col>
        </Row>
      </Panel>
    );
  },
});

export default ConfigurationHelper;
