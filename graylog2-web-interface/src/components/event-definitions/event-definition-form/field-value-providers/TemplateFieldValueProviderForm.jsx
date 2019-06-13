import React from 'react';
import PropTypes from 'prop-types';
import {
  Col,
  ControlLabel,
  DropdownButton,
  FormControl,
  FormGroup,
  HelpBlock,
  InputGroup,
  MenuItem,
  Row,
} from 'react-bootstrap';
import lodash from 'lodash';

class TemplateFieldValueProviderForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  static type = 'template-v1';

  handleValueChange = (event) => {
    const { config, onChange } = this.props;
    const nextTemplate = event.target.value;
    const nextProviders = lodash.cloneDeep(config.providers);
    const templateProvider = nextProviders.find(provider => provider.type === TemplateFieldValueProviderForm.type);
    templateProvider.template = nextTemplate;
    onChange(Object.assign({}, config, { providers: nextProviders }));
  };

  handleTypeChange = (nextType) => {
    const { config, onChange } = this.props;
    onChange(Object.assign({}, config, { data_type: nextType }));
  };

  render() {
    const { config } = this.props;
    const provider = config.providers.find(p => p.type === TemplateFieldValueProviderForm.type);

    return (
      <Row className="row-sm">
        <Col md={12}>
          <FormGroup>
            <ControlLabel>Value</ControlLabel>
            <InputGroup>
              <FormControl type="text" onChange={this.handleValueChange} value={provider.template || ''} />
              <DropdownButton componentClass={InputGroup.Button}
                              id="type"
                              title={lodash.upperFirst(config.data_type || 'type')}
                              onSelect={this.handleTypeChange}>
                <MenuItem eventKey="string" active={config.data_type === 'string'}>String</MenuItem>
              </DropdownButton>
            </InputGroup>
            <HelpBlock>Type a text Field Value or use Freemarker syntax to add a dynamic Value.</HelpBlock>
          </FormGroup>
        </Col>
      </Row>
    );
  }
}

export default TemplateFieldValueProviderForm;
