import React from 'react';
import { Row, Col, Panel, Tabs, Tab } from 'react-bootstrap';

import DocumentationLink from 'components/support/DocumentationLink';

import DocsHelper from 'util/DocsHelper';

const RuleHelper = React.createClass({
  ruleTemplate: `rule "function howto"
when
  has_field("transaction_date")
then
  // the following date format assumes there's no time zone in the string
  let new_date = parse_date(tostring($message.transaction_date), "yyyy-MM-dd HH:mm:ss");
  set_field("transaction_year", new_date.year);
end`,

  render() {
    return (
      <Panel header="Rules quick reference">
        <Row className="row-sm">
          <Col md={12}>
            <p style={{ marginTop: 5 }}>
              Read the <DocumentationLink page={DocsHelper.PAGES.PIPELINE_RULES}
                                                 text="full documentation" />{' '}
              to gain a better understanding of how Graylog pipeline rules work.
            </p>
          </Col>
        </Row>
        <Row className="row-sm">
          <Col md={12}>
            <Tabs defaultActiveKey={1} animation={false}>
              <Tab eventKey={1} title="Example">
                <pre style={{ marginTop: 10, whiteSpace: 'pre-wrap' }}>
                  {this.ruleTemplate}
                </pre>
              </Tab>
              <Tab eventKey={2} title="Functions">
                <div className="table-responsive" style={{ marginTop: 10 }}>
                  <table className="table">
                    <thead>
                    <tr>
                      <th>Function</th>
                      <th>Description</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                      <td><code>tobool(any)</code></td>
                      <td>Converts the single parameter to a boolean value using its string value.</td>
                    </tr>
                    <tr>
                      <td><code>todouble(any, [default: double])</code></td>
                      <td>Converts the first parameter to a double floating point value.</td>
                    </tr>
                    <tr>
                      <td><code>tolong(any, [default: long])</code></td>
                      <td>Converts the first parameter to a long integer value.</td>
                    </tr>
                    <tr>
                      <td><code>tostring(any, [default: string])</code></td>
                      <td>Converts the first parameter to its string representation.</td>
                    </tr>
                    <tr>
                      <td><code>capitalize(value: string)</code></td>
                      <td>Capitalizes a String changing the first letter to title case.</td>
                    </tr>
                    <tr>
                      <td><code>uppercase(value: string, [locale: string])</code></td>
                      <td>Converts a String to upper case.</td>
                    </tr>
                    <tr>
                      <td><code>lowercase(value: string, [locale: string])</code></td>
                      <td>Converts a String to lower case.</td>
                    </tr>
                    <tr>
                      <td><code>contains(value: string, search: string, [ignore_case: boolean])</code></td>
                      <td>Checks if a string contains another string.</td>
                    </tr>
                    <tr>
                      <td><code>substring(value: string, start: long, [end: long])</code></td>
                      <td>Returns a substring of <code><span>value</span></code> with the given start and end offsets.
                      </td>
                    </tr>
                    <tr>
                      <td><code>regex(pattern: string, value: string, [group_names: array[string])</code></td>
                      <td>Match a regular expression against a string, with matcher groups.</td>
                    </tr>
                    <tr>
                      <td><code>parse_date(value: string, pattern: string, [timezone: string])</code></td>
                      <td>Parses a date and time from the given string, according to a strict pattern.</td>
                    </tr>
                    <tr>
                      <td><code>flex_parse_date(value: string, [default: DateTime], [timezone: string])</code></td>
                      <td>Attempts to parse a date and time using the Natty date parser.</td>
                    </tr>
                    <tr>
                      <td><code>format_date(value: DateTime, format: string, [timezone: string])</code></td>
                      <td>Formats a date and time according to a given formatter pattern.</td>
                    </tr>
                    <tr>
                      <td><code>parse_json(value: string)</code></td>
                      <td>Parse a string into a JSON tree.</td>
                    </tr>
                    <tr>
                      <td><code>toip(ip: string)</code></td>
                      <td>Converts the given string to an IP object.</td>
                    </tr>
                    <tr>
                      <td><code>cidr_match(cidr: string, ip: IpAddress)</code></td>
                      <td>Checks whether the given IP matches a CIDR pattern.</td>
                    </tr>
                    <tr>
                      <td><code>from_input(id: string | name: string)</code></td>
                      <td>Checks whether the current message was received by the given input.</td>
                    </tr>
                    <tr>
                      <td><code>route_to_stream(id: string | name: string, [message: Message])</code></td>
                      <td>Assigns the current message to the specified stream.</td>
                    </tr>
                    <tr>
                      <td><code>drop_message(message: Message)</code></td>
                      <td>This currently processed message will be removed from the processing pipeline after the rule
                        finishes.
                      </td>
                    </tr>
                    <tr>
                      <td><code>has_field(field: string, [message: Message])</code></td>
                      <td>Checks whether the currently processed message contains the named field.</td>
                    </tr>
                    <tr>
                      <td><code>remove_field(field: string, [message: Message])</code></td>
                      <td>Removes the named field from the currently processed message.</td>
                    </tr>
                    <tr>
                      <td><code>set_field(field: string, value: any, [message: Message])</code></td>
                      <td>Sets the name field to the given value in the currently processed message.</td>
                    </tr>
                    <tr>
                      <td><code>set_fields(fields: Map&lt;string, any&gt;, [message: Message])</code></td>
                      <td>Sets multiple fields to the given values in the currently processed message.</td>
                    </tr>
                    </tbody>
                  </table>
                </div>
                <p>See all functions in the <DocumentationLink page={DocsHelper.PAGES.PIPELINE_FUNCTIONS}
                                                               text="documentation" />.</p>
              </Tab>
            </Tabs>
          </Col>
        </Row>
      </Panel>
    );
  },
});

export default RuleHelper;
