import PropTypes from 'prop-types';
import React from 'react';

import { Button, ButtonToolbar, Col, Nav, NavItem, Row } from 'react-bootstrap';

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
     * &nbsp;&nbsp;{key: 'key2', title: 'Details', component: (&lt;Acomponent2 /&gt;), disabled: true},<br />
     * &nbsp;&nbsp;{key: 'key3', title: 'Preview', component: (&lt;Acomponent3 /&gt;), disabled: true},<br />
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
    /** Indicates if wizard should be rendered in horizontal or vertical */
    horizontal: PropTypes.bool,
    /** Customize the container CSS class used by this component */
    containerClassName: PropTypes.string,
  };

  static defaultProps = {
    children: undefined,
    onStepChange: () => {},
    horizontal: false,
    containerClassName: 'content',
  };

  constructor(props) {
    super(props);

    this.state = {
      selectedStep: props.steps[0].key,
    };
  }

  _wizardChanged = (eventKey) => {
    const { onStepChange } = this.props;
    onStepChange(eventKey);
    this.setState({ selectedStep: eventKey });
  };

  _disableButton = (direction) => {
    const { steps } = this.props;
    const { selectedStep } = this.state;
    const len = steps.length;
    const disabledPosition = direction === 'next' ? (len - 1) : 0;
    const currentPosition = steps.findIndex(step => step.key === selectedStep);
    const otherPosition = direction === 'next' ? (currentPosition + 1) : (currentPosition - 1);
    const otherStep = (steps[otherPosition] || {});
    return steps[disabledPosition].key === selectedStep || otherStep.disabled;
  };

  _onNext = () => {
    const { steps } = this.props;
    this._wizardChanged(steps[this._getSelectedIndex() + 1].key);
  };

  _onPrevious = () => {
    const { steps } = this.props;
    this._wizardChanged(steps[this._getSelectedIndex() - 1].key);
  };

  _getSelectedIndex = () => {
    const { steps } = this.props;
    const { selectedStep } = this.state;
    return steps.map(step => step.key).indexOf(selectedStep);
  };

  _renderVerticalStepNav = () => {
    const { steps } = this.props;
    const { selectedStep } = this.state;
    return (
      <Col md={2} className={WizardStyle.subnavigation}>
        <Nav stacked bsStyle="pills" activeKey={selectedStep} onSelect={this._wizardChanged}>
          {steps.map((navItem) => {
            return (<NavItem key={navItem.key} eventKey={navItem.key} disabled={navItem.disabled}>{navItem.title}</NavItem>);
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
    );
  };

  _renderHorizontalStepNav = () => {
    const { selectedStep } = this.state;
    const { steps } = this.props;
    return (
      <Col sm={12} className={WizardStyle.horizontal}>
        <div className="pull-right">
          <ButtonToolbar className={WizardStyle.horizontalPreviousNextButtons}>
            <Button onClick={this._onPrevious} bsSize="xsmall" bsStyle="info" disabled={this._disableButton('previous')}>
              <i className="fa fa-caret-left" />
            </Button>
            <Button onClick={this._onNext} bsSize="xsmall" bsStyle="info" disabled={this._disableButton('next')}>
              <i className="fa fa-caret-right" />
            </Button>
          </ButtonToolbar>
        </div>
        <Nav bsStyle="pills" activeKey={selectedStep} onSelect={this._wizardChanged}>
          {steps.map((navItem) => {
            return (<NavItem key={navItem.key} eventKey={navItem.key} disabled={navItem.disabled}>{navItem.title}</NavItem>);
          })}
        </Nav>
      </Col>
    );
  };

  render() {
    const { steps, horizontal, containerClassName, children } = this.props;
    const rightComponentCols = horizontal ? 5 : 3; // If horizontal, use more space for this component
    return (
      <Row className={containerClassName}>
        {horizontal ? this._renderHorizontalStepNav() : this._renderVerticalStepNav()}
        <Col md={7}>
          {steps[this._getSelectedIndex()].component}
        </Col>
        {children && (
          <Col md={rightComponentCols}>
            {children}
          </Col>
        )}
      </Row>
    );
  }
}

export default Wizard;
