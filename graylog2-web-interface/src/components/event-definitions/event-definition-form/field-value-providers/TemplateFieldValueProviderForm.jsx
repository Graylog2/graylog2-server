import React from 'react';
import PropTypes from 'prop-types';
import {
  Col,
  Row,
} from 'react-bootstrap';
import lodash from 'lodash';

import { Input } from 'components/bootstrap';
import { ExternalLink } from 'components/common';

class TemplateFieldValueProviderForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  static type = 'template-v1';

  handleChange = (event) => {
    const { config, onChange } = this.props;
    const nextTemplate = event.target.value;
    const nextProviders = lodash.cloneDeep(config.providers);
    const templateProvider = nextProviders.find(provider => provider.type === TemplateFieldValueProviderForm.type);
    templateProvider.template = nextTemplate;
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
        </Col>
      </Row>
    );
  }
}

export default TemplateFieldValueProviderForm;
