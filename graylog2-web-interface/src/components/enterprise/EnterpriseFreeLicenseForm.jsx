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
import { isEmpty, trim } from 'lodash';

import { Button, ButtonToolbar, Col, Row } from 'components/graylog';
import { Input } from 'components/bootstrap';

const FORM_FIELDS = ['firstName', 'lastName', 'email', 'phone', 'company'];

const PHONE_PATTERN = /^[0-9\-.()+ ]{5,}$/;

export default class EnterpriseFreeLicenseForm extends React.Component {
  static propTypes = {
    onSubmit: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      formFields: {
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        company: '',
      },
      errors: {},
      isSubmitting: false,
    };
  }

  clearValues = (callback) => {
    const clearedFields = FORM_FIELDS.reduce((acc, key) => Object.assign(acc, { [key]: '' }), {});

    this.setState({ formFields: clearedFields }, callback);
  };

  validatePhone = (key, value) => {
    if (isEmpty(value.replace(' ', ''))) {
      return 'Valid phone number  w/ country code is required.';
    }

    return PHONE_PATTERN.test(value) ? null : 'Invalid phone number';
  };

  handleInput = (key, validator) => {
    return (event) => {
      const { formFields, errors } = this.state;
      const value = event.target.value.replace(/[\s]{2,}/, ' '); // strip extraneous spaces
      const newFormFields = Object.assign(formFields, { [key]: value });
      const newErrors = { ...errors };

      if (validator && (typeof validator === 'function')) {
        const errorMessage = validator(key, newFormFields[key]);

        if (errorMessage) {
          newErrors[key] = errorMessage;
        } else {
          delete newErrors[key];
        }
      }

      this.setState({ formFields: newFormFields, errors: newErrors });
    };
  };

  validationState = (key) => {
    const { errors } = this.state;

    return errors[key] ? 'error' : null;
  };

  validationMessage = (key) => {
    const { errors } = this.state;

    return errors[key];
  };

  formIsInvalid = () => {
    const { isSubmitting, formFields, errors } = this.state;

    return isSubmitting || !isEmpty(errors) || !isEmpty(FORM_FIELDS.filter((key) => isEmpty(trim(formFields[key]))));
  };

  submitForm = (event) => {
    event.preventDefault();

    const { onSubmit } = this.props;
    const { formFields } = this.state;

    // First set "submitting" status to make sure we disable the submit button (avoid double-click)
    this.setState({ isSubmitting: true }, () => {
      onSubmit(formFields, (success) => {
        if (success) {
          // Clear form before unsetting "submitting" status, again, to avoid double-click
          this.clearValues(() => {
            this.setState({ isSubmitting: false });
          });
        } else {
          this.setState({ isSubmitting: false });
        }
      });
    });
  };

  resetForm = () => {
    this.clearValues();
  };

  render() {
    const { formFields: { firstName, lastName, company, email, phone } } = this.state;

    return (
      <form onSubmit={this.submitForm}>
        <Row>
          <Col md={12}>
            <Input type="text"
                   id="firstName"
                   label="First Name"
                   value={firstName}
                   required
                   onChange={this.handleInput('firstName')} />
            <Input type="text"
                   id="lastName"
                   label="Last Name"
                   value={lastName}
                   required
                   onChange={this.handleInput('lastName')} />
            <Input type="text"
                   id="company"
                   label="Company"
                   value={company}
                   required
                   onChange={this.handleInput('company')} />
            <Input type="email"
                   id="email"
                   label="Email Address"
                   value={email}
                   placeholder="Please provide a valid email address to send the license key to"
                   title="Please provide a valid email address to send the license key to"
                   required
                   onChange={this.handleInput('email')} />
            <Input type="tel"
                   id="phone"
                   label="Phone Number"
                   value={phone}
                   placeholder="Please provide your phone number w/ country code"
                   title="Please provide your phone number w/ country code"
                   help={this.validationMessage('phone')}
                   bsStyle={this.validationState('phone')}
                   required
                   onChange={this.handleInput('phone', this.validatePhone)} />
          </Col>
        </Row>
        <Row>
          <Col sm={11}>
            <ButtonToolbar>
              <Button id="submit-entry"
                      disabled={this.formIsInvalid()}
                      type="submit"
                      bsSize="small"
                      bsStyle="primary">
                UPGRADE NOW
              </Button>
              <Button id="clear-entry"
                      onClick={this.resetForm}
                      bsSize="small">
                Clear form
              </Button>
            </ButtonToolbar>
          </Col>
        </Row>
      </form>
    );
  }
}
