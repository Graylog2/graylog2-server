/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
/* eslint-disable react/no-unescaped-entities, no-template-curly-in-string */
import React from 'react';

import { Alert, Col, Row } from 'components/graylog';

const HTTPJSONPathAdapterDocumentation = () => {
  const exampleJSON = `{
  "user": {
    "login": "jane",
    "full_name": "Jane Doe",
    "roles": ["admin", "developer"],
    "contact": {
      "email": "jane@example.com",
      "cellphone": "+49123456789"
    }
  }
}`;
  const noMultiResult = '{"value": "Jane Doe"}';
  const mapResult = `{
  "login": "jane",
  "full_name": "Jane Doe",
  "roles": ["admin", "developer"],
  "contact": {
    "email": "jane@example.com",
    "cellphone": "+49123456789"
  }
}`;
  const smallMapResult = `{
  "email": "jane@example.com",
  "cellphone": "+49123456789"
}`;
  const listResult = `{
  "value": ["admin", "developer"]
}`;
  const pipelineRule = `rule "lookup user"
when has_field("user_login")
then
  // Get the user login from the message
  let userLogin = to_string($message.user_login);
  // Lookup the single value, in our case the full name, in the user-api lookup table
  let userName = lookup_value("user-api", userLogin);
  // Set the field "user_name" in the message
  set_field("user_name", userName)

  // Lookup the multi value in the user-api lookup table
  let userData = lookup("user-api", userLogin);
  // Set the email and cellphone as fields in the message
  set_field("user_email", userData["email"]);
  set_field("user_cellphone", userData["cellphone"]);
end`;

  return (
    <div>
      <p>
        The HTTPJSONPath data adapter executes <em>HTTP GET</em> requests to lookup a key and parses the result based on
        configured JSONPath expressions.
      </p>

      <Alert style={{ marginBottom: 10 }} bsStyle="info">
        Every lookup table result has two values. A <em>single value</em> and a <em>multi value</em>. The single
        value will be used when the lookup result is expected to be a string, number or boolean. The multi value
        will be used when the lookup result is expected to be a map or list.
      </Alert>

      <h3 style={{ marginBottom: 10 }}>Configuration</h3>

      <h5 style={{ marginBottom: 10 }}>Lookup URL</h5>
      <p style={{ marginBottom: 10, padding: 0 }}>
        The URL that will be used for the HTTP request. To use the <em>lookup key</em> in the URL, the
        <code>{'${key}'}</code>
        value can be used. This variable will be replaced by the actual key that is passed to a lookup function. <br />
        (example: <code>{'https://example.com/api/lookup?key=${key}'}</code>)
      </p>

      <h5 style={{ marginBottom: 10 }}>Single value JSONPath</h5>
      <p style={{ marginBottom: 10, padding: 0 }}>
        This JSONPath expression will be used to parse the <em>single value</em> of the lookup result.
        (example: <code>$.user.full_name</code>)
      </p>

      <h5 style={{ marginBottom: 10 }}>Multi value JSONPath</h5>
      <p style={{ marginBottom: 10, padding: 0 }}>
        This JSONPath expression will be used to parse the <em>multi value</em> of the lookup result.
        (example: <code>$.users[*]</code>)
        The multi value JSONPath setting is <em>optional</em>. Without it, the single value is also present in the
        multi value result.
      </p>

      <h5 style={{ marginBottom: 10 }}>HTTP User-Agent</h5>
      <p style={{ marginBottom: 10, padding: 0 }}>
        This is the <em>User-Agent</em> header that will be used for the HTTP requests. You should include some
        contact details so owners of the services you query know whom to contact if issues arise.
        (like excessive API requests from your Graylog cluster)
      </p>

      <hr />

      <h3 style={{ marginBottom: 10 }}>Example</h3>
      <p>
        This shows an example configuration and the values that will be returned from a lookup.<br />
        The configured URL is <strong>{'https://example.com/api/users/${key}'}</strong> and the <code>{'${key}'}</code>
        gets replaced by <strong>jane</strong> during the lookup request.
      </p>
      <p>
        This is the resulting JSON document:
      </p>
      <pre>{exampleJSON}</pre>

      <Row>
        <Col md={4}>
          <h5 style={{ marginBottom: 10 }}>Configuration</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            Single value JSONPath: <code>$.user.full_name</code><br />
            Multi value JSONPath: <em>empty</em><br />
          </p>
        </Col>
        <Col md={8}>
          <h5 style={{ marginBottom: 10 }}>Result</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            Single value: <code>Jane Doe</code><br />
            Multi value:
            <pre>{noMultiResult}</pre>
          </p>
        </Col>
      </Row>
      <Row>
        <Col md={4}>
          <h5 style={{ marginBottom: 10 }}>Configuration</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            Single value JSONPath: <code>$.user.full_name</code><br />
            Multi value JSONPath: <code>$.user</code><br />
          </p>
        </Col>
        <Col md={8}>
          <h5 style={{ marginBottom: 10 }}>Result</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            Single value: <code>Jane Doe</code><br />
            Multi value:
            <pre>{mapResult}</pre>
          </p>
        </Col>
      </Row>
      <Row>
        <Col md={4}>
          <h5 style={{ marginBottom: 10 }}>Configuration</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            Single value JSONPath: <code>$.user.contact.email</code><br />
            Multi value JSONPath: <code>$.user.roles[*]</code><br />
          </p>
        </Col>
        <Col md={8}>
          <h5 style={{ marginBottom: 10 }}>Result</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            Single value: <code>jane@example.com</code><br />
            Multi value:
            <pre>{listResult}</pre>
          </p>
        </Col>
      </Row>
      <Row>
        <Col md={4}>
          <h5 style={{ marginBottom: 10 }}>Configuration</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            Single value JSONPath: <code>$.user.full_name</code><br />
            Multi value JSONPath: <code>$.user.contact</code><br />
          </p>
        </Col>
        <Col md={8}>
          <h5 style={{ marginBottom: 10 }}>Result</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            Single value: <code>Jane Doe</code><br />
            Multi value:
            <pre>{smallMapResult}</pre>
          </p>
        </Col>
      </Row>

      <h5 style={{ marginBottom: 10 }}>Pipeline Rule</h5>
      <p>
        This is an example pipeline rule that uses the example data from our last configuration example.
      </p>
      <pre>{pipelineRule}</pre>
    </div>
  );
};

export default HTTPJSONPathAdapterDocumentation;
