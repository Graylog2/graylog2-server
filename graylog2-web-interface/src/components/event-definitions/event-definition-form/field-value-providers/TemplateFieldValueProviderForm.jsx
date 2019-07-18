import React from 'react';
import PropTypes from 'prop-types';
import { Checkbox, Col, FormGroup, HelpBlock, Row } from 'react-bootstrap';
import lodash from 'lodash';

import { Input } from 'components/bootstrap';
import { ExternalLink } from 'components/common';
import FormsUtils from 'util/FormsUtils';

class TemplateFieldValueProviderForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  static type = 'template-v1';

  handleChange = (event) => {
    const { config, onChange } = this.props;
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);
    const nextProviders = lodash.cloneDeep(config.providers);
    const templateProvider = nextProviders.find(provider => provider.type === TemplateFieldValueProviderForm.type);
    templateProvider[name] = value;
    onChange(Object.assign({}, config, { providers: nextProviders }));
  };

  render() {
    const { config } = this.props;
    const provider = config.providers.find(p => p.type === TemplateFieldValueProviderForm.type);

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
        <Col md={12}>
          <Input id="template-provider-template"
                 name="template"
                 type="text"
                 label="Template"
                 onChange={this.handleChange}
                 value={provider.template || ''}
                 help={helpText} />

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
      </Row>
    );
  }
}

export default TemplateFieldValueProviderForm;
