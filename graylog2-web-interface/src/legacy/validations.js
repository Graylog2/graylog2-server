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
import $ from 'jquery';

import DateTime from 'logic/datetimes/DateTime';

global.jQuery = $;
require('bootstrap/js/tooltip');
require('bootstrap/js/popover');

export function validate(formContainer) {
  let errors = false;

  $('.validatable', formContainer).each(function () {
    // Do not check disabled form fields.
    if (!$(this).is(':disabled')) {
      const dataValidations = $(this).attr('data-validate');

      if (dataValidations !== undefined) {
        const validatorTypes = dataValidations.split(' ');

        for (let i = 0; (!errors && i < validatorTypes.length); i++) {
          errors = dispatchRuleValidation($(this), validatorTypes[i]);
        }
      }
    }
  });

  return !errors;
}

function dispatchRuleValidation($ref, validatorType) {
  let errors = false;

  switch (validatorType) {
    case 'defined':
      if (!validateDefined($ref)) {
        validationFailure($ref, 'Must be set');
        errors = true;
      }

      break;
    case 'port_number':
      if (!validatePortNumber($ref)) {
        validationFailure($ref, 'Must be a valid port number');
        errors = true;
      }

      break;
    case 'number':
      if (!validateNumber($ref)) {
        validationFailure($ref, 'Must be a number');
        errors = true;
      }

      break;
    case 'positive_number':
      if (!validatePositiveNumber($ref)) {
        validationFailure($ref, 'Must be a positive number');
        errors = true;
      }

      break;
    case 'negative_number':
      if (!validateNegativeNumber($ref)) {
        validationFailure($ref, 'Must be a negative number');
        errors = true;
      }

      break;
    case 'not_negative_number':
      if (!validateNotNegativeNumber($ref)) {
        validationFailure($ref, 'Must be a not negative number');
        errors = true;
      }

      break;
    case 'alphanum_underscore':
      if (!validateAlphanumericUnderscores($ref)) {
        validationFailure($ref, 'Must only contain alphanumeric characters or underscores');
        errors = true;
      }

      break;
    case 'datetime_format':
      if (!validateDatetimeFormat($ref)) {
        validationFailure($ref, 'Is not in a valid format');
        errors = true;
      }

      break;
    case 'timerange':
      if (!validateAbsoluteTimerange($ref)) {
        validationFailure($ref, "cannot be earlier than 'From'");
        errors = true;
      }

      break;
  }

  return errors;
}

function validationFailure($el, msg) {
  $el.popover({
    content: msg,
    placement: 'bottom',
    trigger: 'manual',
  });

  $el.on('shown.bs.popover', () => {
    window.setTimeout(() => {
      $el.popover('destroy');
    }, 3000);
  });

  $el.popover('show');
}

// Validators.
function validateDefined(el) {
  return el.val() != undefined && el.val().length > 0;
}

function validatePortNumber(el) {
  const i = parseInt(el.val());

  return i > 0 && i < 65535;
}

function validatePositiveNumber(el) {
  return parseInt(el.val()) > 0;
}

function validateNegativeNumber(el) {
  return parseInt(el.val()) < 0;
}

function validateAlphanumericUnderscores(el) {
  return el.val().match('^\\w*$');
}

function validateNotNegativeNumber(el) {
  return (validatePositiveNumber(el) || parseInt(el.val()) == 0);
}

function validateNumber(el) {
  return isNumber(el.val());
}

function validateDatetimeFormat(el) {
  const dateString = $(el).val();

  try {
    DateTime.parseFromString(dateString);

    return true;
  } catch (e) {
    // Do nothing
  }

  return false;
}

function validateAbsoluteTimerange(el) {
  const parent = $(el).parent().parent();
  const fromStr = $("input[name='from']", parent).val();
  const toStr = $("input[name='to']", parent).val();

  try {
    const from = DateTime.parseFromString(fromStr).toMoment();
    const to = DateTime.parseFromString(toStr).toMoment();

    return (from.isBefore(to) || from.isSame(to));
  } catch (e) {
    // Do nothing
  }

  return false;
}
