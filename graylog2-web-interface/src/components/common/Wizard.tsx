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
import * as React from 'react';
import find from 'lodash/find';
import styled, { css } from 'styled-components';

import { Button, ButtonToolbar, Col, Nav, NavItem, Row } from 'components/bootstrap';
import type { SelectCallback } from 'components/bootstrap/types';

import Icon from './Icon';

const SubnavigationCol = styled(Col)(({ theme }) => css`
  border-right: ${theme.colors.gray[80]} solid 1px;
`);

const HorizontalCol = styled(Col)`
  margin-bottom: 15px;
`;

const StyledNav: React.ComponentType<any> = styled(Nav)<{$style?: 'stepper'}>(({ $style, theme }) => css`
  ${$style === 'stepper' ? `
  &.nav {
   counter-reset: line-number;
    > li {
      counter-increment: line-number;
      > a {
        position: relative;
        display: flex;
        padding: 10px 0px;
        justify-content: center;
        align-items: center;
        &:hover,
        &:focus {
          background-color: initial;
        }
        > div {
          flex-shrink: 0;
        }
        
        &::before {
          display: flex;
          justify-content: center;
          align-items: center;
          flex-grow: 0; 
          flex-shrink: 0;
          background-color: ${theme.colors.global.contentBackground};
          border-color: ${theme.colors.variant.lighter.default};
          margin-right: 10px;
          content: counter(line-number);
          width: 35px;
          height: 35px;
          border-radius: 50%;
          border: 2px solid;
          z-index: 2;
        }
        &::after {
          display: flex;
          justify-content: center;
          align-items: center;
          background-color: ${theme.colors.global.contentBackground};
          border-color: ${theme.colors.variant.lighter.default};
          margin-right: 10px;
          content: ' ';
          border: 1px solid ${theme.colors.input.border};
          width: 100%;
          flex-shrink: 1;
          flex-grow: 0;
          align-self: center;
          margin: 0 16px;
        } 
        &:hover::after {
          background-color: ${theme.colors.variant.lightest.default};
        }
      }
      &:last-child > a {
       justify-content: flex-start;
       &::after {
        display:none;
       } 
      }
      &.disabled > a {
        color: ${theme.colors.variant.light.default};

        &:hover,
        &:focus {
          color: ${theme.colors.variant.light.default};
        }
      }
    }

    .open > a {
      &,
      &:hover,
      &:focus {
        background-color: inital;
        border-color: ${theme.colors.variant.primary};
      }
    }
    &.nav-justified {
     > li {
      > a {
        text-align: left;
      }
     }
    }
    &.nav-pills {
      > li {
        > a {
          color: initial;

          &:hover  {
            color: ${theme.colors.global.link};
          }
        }

        &.active > a {
          &,
          &:hover,
          &:focus {
            color: ${theme.colors.global.link};
            background-color: initial; 
          }
        }
      }
    } 
  }
  ` : `&.nav {
    > li {
      border: 1px solid ${theme.colors.variant.lighter.default};
      border-left: 0;

      &:first-child {
        border-left: 1px solid ${theme.colors.variant.lighter.default};
        border-radius: 4px 0 0 4px;

        > a {
          border-radius: 4px 0 0 4px;
        }
      }

      &:last-child {
        border-radius: 0 4px 4px 0;

        > a {
          border-radius: 0 4px 4px 0;
        }
      }

      &:not(:last-child) > a {
        &::after {
          transition: background-color 150ms ease-in-out;
          background-color: ${theme.colors.global.contentBackground};
          border-color: ${theme.colors.variant.lighter.default};
          border-style: solid;
          border-width: 0 1px 1px 0;
          content: '';
          display: block;
          height: 15px;
          position: absolute;
          right: -1px;
          top: 50%;
          transform: translateY(-50%) translateX(50%) rotate(-45deg);
          width: 15px;
          z-index: 2;
        }

        &:hover::after {
          background-color: ${theme.colors.variant.lightest.default};
        }
      }

      &.active a {
        &,
        &:hover,
        &::after,
        &:hover::after {
          background-color: ${theme.colors.global.link};
        }
      }

      > a {
        border-radius: 0;
      }
    }
  }

  @media (max-width: ${theme.breakpoints.max.md}) {
    &.nav {
      > li {
        border-right: 0;
        border-left: 0;

        &:last-child a,
        &:first-child a {
          border-radius: 0;
        }

        &:not(:last-child) {
          border-bottom: 0;
        }

        &:not(:last-child) > a {
          &::after {
            bottom: 0;
            left: 50%;
            top: auto;
            width: 10px;
            height: 10px;
            transform: translateY(50%) translateX(-50%) rotate(45deg);
          }
        }
      }

      &.nav-justified > li > a {
        margin-bottom: 0;
      }
    }
  }`}
`);

const HorizontalButtonToolbar = styled(ButtonToolbar)`
  padding: 7px;
`;

const isValidActiveStep = (activeStep: StepKey | null | undefined, steps: StepsType) => {
  if (activeStep === undefined || activeStep === null) {
    return false;
  }

  return find(steps, { key: activeStep });
};

const warnOnInvalidActiveStep = (activeStep: StepKey | null | undefined, steps: StepsType) => {
  if (activeStep === undefined || activeStep === null) {
    return;
  }

  if (!isValidActiveStep(activeStep, steps)) {
    // eslint-disable-next-line no-console
    console.warn(`activeStep ${activeStep} is not a key in any element of the 'steps' prop!`);
  }
};

export type StepKey = number | string;

export type StepType = {
  key: StepKey,
  title: React.ReactNode,
  component: React.ReactElement,
  disabled?: boolean,
};

export type StepsType = Array<StepType>;
type Props = {
  steps: StepsType,
  activeStep: StepKey | null | undefined,
  onStepChange: (StepKey) => void,
  children: React.ReactNode,
  horizontal: boolean,
  justified: boolean,
  containerClassName: string,
  hidePreviousNextButtons: boolean,
  style: 'stepper' | undefined,
};

type State = {
  selectedStep: StepKey,
};

/**
 * Component that renders a wizard, letting the consumers of the component
 * manage the state. It will render at least two columns: First column will hold
 * the steps the wizard will take. Second column will render the component of the
 * selected step. In a optional third column the consumer can render a preview.
 */
class Wizard extends React.Component<Props, State> {
  static defaultProps = {
    children: undefined,
    activeStep: undefined,
    onStepChange: () => {},
    horizontal: false,
    justified: false,
    containerClassName: 'content',
    hidePreviousNextButtons: false,
    style: undefined,
  };

  constructor(props: Props) {
    super(props);

    warnOnInvalidActiveStep(props.activeStep, props.steps);

    this.state = {
      selectedStep: props.steps[0].key,
    };
  }

  componentDidUpdate() {
    const { activeStep, steps } = this.props;

    warnOnInvalidActiveStep(activeStep, steps);
  }

  _getSelectedStep = () => {
    const { activeStep, steps } = this.props;
    const { selectedStep } = this.state;

    return (isValidActiveStep(activeStep, steps) ? activeStep : selectedStep);
  };

  _wizardChanged = (eventKey: StepKey) => {
    const { activeStep, onStepChange } = this.props;

    onStepChange(eventKey);

    // If activeStep is given, component should behave in a controlled way and let consumer decide which step to render.
    if (!activeStep) {
      this.setState({ selectedStep: eventKey });
    }
  };

  _disableButton = (direction: 'previous' | 'next') => {
    const { steps } = this.props;
    const selectedStep = this._getSelectedStep();
    const len = steps.length;
    const disabledPosition = direction === 'next' ? (len - 1) : 0;
    const currentPosition = steps.findIndex((step) => step.key === this._getSelectedStep());
    const otherPosition = direction === 'next' ? (currentPosition + 1) : (currentPosition - 1);
    const otherStep = (steps[otherPosition]);

    return steps[disabledPosition].key === selectedStep || otherStep?.disabled;
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
    const selectedStep = this._getSelectedStep();

    return steps.map((step) => step.key).indexOf(selectedStep);
  };

  _renderVerticalStepNav = () => {
    const { justified, steps, hidePreviousNextButtons, style } = this.props;
    const selectedStep = this._getSelectedStep();

    return (
      <SubnavigationCol md={2}>
        <StyledNav stacked
                   bsStyle="pills"
                   $style={style}
                   activeKey={selectedStep}
                   onSelect={this._wizardChanged as SelectCallback}
                   justified={justified}>
          {steps.map((navItem) => (
            <NavItem key={navItem.key} eventKey={navItem.key} disabled={navItem.disabled}>{navItem.title}</NavItem>
          ))}
        </StyledNav>
        {!hidePreviousNextButtons && (
          <>
            <br />
            <Row>
              <Col xs={6}>
                <Button onClick={this._onPrevious}
                        bsSize="small"
                        bsStyle="info"
                        disabled={this._disableButton('previous')}>Previous
                </Button>
              </Col>
              <Col className="text-right" xs={6}>
                <Button onClick={this._onNext}
                        bsSize="small"
                        bsStyle="info"
                        disabled={this._disableButton('next')}>Next
                </Button>
              </Col>
            </Row>
          </>
        )}
      </SubnavigationCol>
    );
  };

  _renderHorizontalStepNav = () => {
    const selectedStep = this._getSelectedStep();
    const { justified, steps, hidePreviousNextButtons, style } = this.props;

    return (
      <HorizontalCol sm={12}>
        {!hidePreviousNextButtons && (
          <div className="pull-right">
            <HorizontalButtonToolbar>
              <Button onClick={this._onPrevious}
                      aria-label="Previous"
                      bsSize="xsmall"
                      bsStyle="info"
                      disabled={this._disableButton('previous')}>
                <Icon name="arrow_left" />
              </Button>
              <Button onClick={this._onNext}
                      aria-label="Next"
                      bsSize="xsmall"
                      bsStyle="info"
                      disabled={this._disableButton('next')}>
                <Icon name="arrow_right" />
              </Button>
            </HorizontalButtonToolbar>
          </div>
        )}
        <StyledNav bsStyle="pills"
                   activeKey={selectedStep}
                   $style={style}
                   onSelect={this._wizardChanged as SelectCallback}
                   justified={justified}>
          {steps.map((navItem) => (
            <NavItem key={navItem.key} eventKey={navItem.key} disabled={navItem.disabled}>{navItem.title}</NavItem>))}
        </StyledNav>
      </HorizontalCol>
    );
  };

  render() {
    const { steps, horizontal, containerClassName, children } = this.props;
    let leftComponentCols;

    if (children) {
      leftComponentCols = 7;
    } else {
      leftComponentCols = horizontal ? 12 : 10;
    }

    const rightComponentCols = horizontal ? 5 : 3; // If horizontal, use more space for this component

    return (
      <Row className={containerClassName}>
        {horizontal ? this._renderHorizontalStepNav() : this._renderVerticalStepNav()}
        <Col md={leftComponentCols}>
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
