import PropTypes from 'prop-types';
import React from 'react';

import { Row, Button, Col, Nav, NavItem } from 'react-bootstrap';

import WizardStyle from './Wizard.css';

/**
 * Component that renders a wizard, letting the consumers of the component
 * manage the state. It will render at least two columns: First column will hold
 * the steps the wizard will take. Second column will render the component of the
 * selected step. In a optional third column the consumer can render a preview.
 */
class Wizard extends React.Component {
  static propTypes = {
    /**
     * Array of objects which will describe the wizard. The object must
     * contain a unique 'key' attribute, a 'title' which will be shown as step link on the left side and
     * a 'component' attribute which will hold the component which is to render for the step.
     *
     * e.g:
     * [\[<br />
     * &nbsp;&nbsp;{key: 'key1', title: 'General Information', component: (&lt;Acomponent1 /&gt;)},<br />
     * &nbsp;&nbsp;{key: 'key2', title: 'Details', component: (&lt;Acomponent2 /&gt;)},<br />
     * &nbsp;&nbsp;{key: 'key3', title: 'Preview', component: (&lt;Acomponent3 /&gt;)},<br />
     * \]]
     */
    steps: PropTypes.arrayOf(PropTypes.object).isRequired,
    /**
     * Callback which is called when the user changes the step. As an argument the callback gets the key
     * of the next step.
     */
    onStepChange: PropTypes.func,
    /** Optional component which can be rendered on the right side e.g a preview */
    children: PropTypes.element,
  };

  static defaultProps = {
    children: undefined,
    onStepChange: () => {},
  };

  constructor(props) {
    super(props);

    this.state = {
      selectedStep: props.steps[0].key,
    };
  }

  _wizardChanged = (eventKey) => {
    this.props.onStepChange(eventKey);
    this.setState({ selectedStep: eventKey });
  };

  _disableButton = (direction) => {
    const len = this.props.steps.length;
    const position = direction === 'next' ? (len - 1) : 0;
    return this.props.steps[position].key === this.state.selectedStep;
  };

  _onNext = () => {
    this._wizardChanged(this.props.steps[this._getSelectedIndex() + 1].key);
  };

  _onPrevious = () => {
    this._wizardChanged(this.props.steps[this._getSelectedIndex() - 1].key);
  };

  _getSelectedIndex = () => {
    return this.props.steps.map(step => step.key).indexOf(this.state.selectedStep);
  };

  render() {
    return (
      <Row className="content">
        <Col md={2} className={WizardStyle.subnavigation}>
          <Nav stacked bsStyle="pills" activeKey={this.state.selectedStep} onSelect={this._wizardChanged}>
            {this.props.steps.map((navItem) => {
              return (<NavItem key={navItem.key} eventKey={navItem.key}>{navItem.title}</NavItem>);
            })}
          </Nav>
          <br />
          <Row>
            <Col xs={6}>
              <Button onClick={this._onPrevious} bsSize="small" bsStyle="info" disabled={this._disableButton('previous')}>Previous</Button>
            </Col>
            <Col className="text-right" xs={6}>
              <Button onClick={this._onNext} bsSize="small" bsStyle="info" disabled={this._disableButton('next')}>Next</Button>
            </Col>
          </Row>
        </Col>
        <Col md={7}>
          {this.props.steps[this._getSelectedIndex()].component}
        </Col>
        {this.props.children &&
          <Col md={3}>
            {this.props.children}
          </Col>
        }
      </Row>);
  }
}

export default Wizard;
