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


class TemplateFieldValueProviderForm extends React.Component {
  static propTypes = {};

  render() {
    return (
      <Row className="row-sm">
        <Col md={12}>
          <FormGroup>
            <ControlLabel>Value</ControlLabel>
            <InputGroup>
              <FormControl type="text" onChange={() => {}} />
              <DropdownButton componentClass={InputGroup.Button}
                              id="type"
                              title="Type">
                <MenuItem eventKey="1" selected>String</MenuItem>
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
