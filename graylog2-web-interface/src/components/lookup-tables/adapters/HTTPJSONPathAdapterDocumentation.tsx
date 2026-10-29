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
import React from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { Col, Row, MantineAccordion } from 'components/bootstrap';

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

const jsonPathFullName = '$.user.full_name';
const jsonPathUser = '$.user';
const jsonPathRoles = '$.user.roles[*]';
const jsonPathContact = '$.user.contact';
const jsonPathContactEmail = '$.user.contact.email';
const exampleName = 'Jane Doe';
const exampleEmail = 'jane@example.com';

const HTTPJSONPathAdapterDocumentation = () => {
  const { t } = useTranslation('lookup_tables');
  const accordionItems = [
    {
      value: 'lookup_url',
      label: t('http_doc.lookup_url_label'),
      content: (
        <div>
          <p style={{ marginBottom: 10, padding: 0 }}>
            <Trans t={t} i18nKey="http_doc.lookup_url_text" components={{ emphasis: <em />, code: <code /> }} />{' '}
            <br />
            <Trans t={t} i18nKey="http_doc.lookup_url_example" components={{ code: <code /> }} />
          </p>
        </div>
      ),
    },
    {
      value: 'single_value',
      label: t('http_doc.single_value_label'),
      content: (
        <div>
          <p style={{ marginBottom: 10, padding: 0 }}>
            <Trans t={t} i18nKey="http_doc.single_value_text" components={{ emphasis: <em />, code: <code /> }} />
          </p>
        </div>
      ),
    },
    {
      value: 'multi_value',
      label: t('http_doc.multi_value_label'),
      content: (
        <div>
          <p style={{ marginBottom: 10, padding: 0 }}>
            <Trans t={t} i18nKey="http_doc.multi_value_text" components={{ emphasis: <em />, code: <code /> }} />
          </p>
        </div>
      ),
    },
    {
      value: 'http_useragent',
      label: t('http_doc.http_useragent_label'),
      content: (
        <div>
          <p style={{ marginBottom: 10, padding: 0 }}>
            <Trans t={t} i18nKey="http_doc.http_useragent_text" components={{ emphasis: <em /> }} />
          </p>
        </div>
      ),
    },
  ];

  return (
    <div>
      <p>
        <Trans t={t} i18nKey="http_doc.intro" components={{ emphasis: <em /> }} />
      </p>

      <p>
        <Trans t={t} i18nKey="http_doc.two_values" components={{ emphasis: <em /> }} />
      </p>

      <h3 style={{ marginBottom: 10 }}>{t('shared.configuration')}</h3>

      <MantineAccordion accordionItems={accordionItems} defaultValue="lookup_url" />

      <hr />

      <h3 style={{ marginBottom: 10 }}>{t('http_doc.example_title')}</h3>
      <p>
        {t('http_doc.example_intro')}
        <br />
        <Trans t={t} i18nKey="http_doc.example_url" components={{ bold: <strong />, code: <code /> }} />
      </p>
      <p>{t('http_doc.example_result')}</p>
      <pre>{exampleJSON}</pre>

      <Row>
        <Col md={4}>
          <h5 style={{ marginBottom: 10 }}>{t('shared.configuration')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('http_doc.single_value_jsonpath')} <code>{jsonPathFullName}</code>
            <br />
            {t('http_doc.multi_value_jsonpath')} <em>{t('http_doc.empty')}</em>
            <br />
          </p>
        </Col>
        <Col md={8}>
          <h5 style={{ marginBottom: 10 }}>{t('http_doc.result_title')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('http_doc.single_value_result')} <code>{exampleName}</code>
            <br />
            {t('http_doc.multi_value_result')}
          </p>
          <pre>{noMultiResult}</pre>
        </Col>
      </Row>
      <Row>
        <Col md={4}>
          <h5 style={{ marginBottom: 10 }}>{t('shared.configuration')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('http_doc.single_value_jsonpath')} <code>{jsonPathFullName}</code>
            <br />
            {t('http_doc.multi_value_jsonpath')} <code>{jsonPathUser}</code>
            <br />
          </p>
        </Col>
        <Col md={8}>
          <h5 style={{ marginBottom: 10 }}>{t('http_doc.result_title')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('http_doc.single_value_result')} <code>{exampleName}</code>
            <br />
            {t('http_doc.multi_value_result')}
          </p>
          <pre>{mapResult}</pre>
        </Col>
      </Row>
      <Row>
        <Col md={4}>
          <h5 style={{ marginBottom: 10 }}>{t('shared.configuration')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('http_doc.single_value_jsonpath')} <code>{jsonPathContactEmail}</code>
            <br />
            {t('http_doc.multi_value_jsonpath')} <code>{jsonPathRoles}</code>
            <br />
          </p>
        </Col>
        <Col md={8}>
          <h5 style={{ marginBottom: 10 }}>{t('http_doc.result_title')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('http_doc.single_value_result')} <code>{exampleEmail}</code>
            <br />
            {t('http_doc.multi_value_result')}
          </p>
          <pre>{listResult}</pre>
        </Col>
      </Row>
      <Row>
        <Col md={4}>
          <h5 style={{ marginBottom: 10 }}>{t('shared.configuration')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('http_doc.single_value_jsonpath')} <code>{jsonPathFullName}</code>
            <br />
            {t('http_doc.multi_value_jsonpath')} <code>{jsonPathContact}</code>
            <br />
          </p>
        </Col>
        <Col md={8}>
          <h5 style={{ marginBottom: 10 }}>{t('http_doc.result_title')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('http_doc.single_value_result')} <code>{exampleName}</code>
            <br />
            {t('http_doc.multi_value_result')}
          </p>
          <pre>{smallMapResult}</pre>
        </Col>
      </Row>

      <h5 style={{ marginBottom: 10 }}>{t('http_doc.pipeline_rule_title')}</h5>
      <p>{t('http_doc.pipeline_rule_intro')}</p>
      <pre>{pipelineRule}</pre>
    </div>
  );
};

export default HTTPJSONPathAdapterDocumentation;
