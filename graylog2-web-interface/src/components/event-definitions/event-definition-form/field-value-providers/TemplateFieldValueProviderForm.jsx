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
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Checkbox, Col, FormGroup, HelpBlock, Row } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { ExternalLink } from 'components/common';
import FormsUtils from 'util/FormsUtils';

import TemplateFieldValueProviderPreview from './TemplateFieldValueProviderPreview';

class TemplateFieldValueProviderForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  static type = 'template-v1';

  static defaultConfig = {
    template: '',
  };

  static requiredFields = [
    'template',
  ];

  handleChange = (event) => {
    const { config, onChange } = this.props;
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);
    const nextProviders = lodash.cloneDeep(config.providers);
    const templateProvider = nextProviders.find((provider) => provider.type === TemplateFieldValueProviderForm.type);

    templateProvider[name] = value;
    onChange({ ...config, providers: nextProviders });
  };

  render() {
    const { config, validation } = this.props;
    const provider = config.providers.find((p) => p.type === TemplateFieldValueProviderForm.type);

    const helpText = (
      <span>
        Type a literal text to set to this Field or use{' '}
        <ExternalLink href="https://cdn.rawgit.com/DJCordhose/jmte/master/doc/index.html">
          JMTE syntax
        </ExternalLink>
        {' '}to add a dynamic Value.
      </span>
    );

    return (
      <Row className="row-sm">
        <Col md={7} lg={6}>
          <Input id="template-provider-template"
                 name="template"
                 type="text"
                 label="Template"
                 onChange={this.handleChange}
                 value={provider.template || ''}
                 bsStyle={validation.errors.template ? 'error' : null}
                 help={validation.errors.template || helpText} />

          <FormGroup>
            <Checkbox id="lookup-message-require-values"
                      name="require_values"
                      checked={provider.require_values}
                      onChange={this.handleChange}>
              Require all template values to be set
            </Checkbox>
            <HelpBlock>Check this option to validate that all variables used in the Template have values.</HelpBlock>
          </FormGroup>
        </Col>
        <Col md={5} lgOffset={1}>
          <TemplateFieldValueProviderPreview />
        </Col>
      </Row>
    );
  }
}

export default TemplateFieldValueProviderForm;
