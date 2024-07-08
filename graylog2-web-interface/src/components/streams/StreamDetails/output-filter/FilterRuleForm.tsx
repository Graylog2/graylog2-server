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
import { Formik } from 'formik';

import { BootstrapModalWrapper, Modal } from 'components/bootstrap';
import { FormikInput, ModalSubmit } from 'components/common';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';
import FilterRulesFields from 'components/streams/StreamDetails/output-filter/FilterRulesFields';

type StreamOutputFilterRuleValues = Partial<StreamOutputFilterRule>;
type Props = {
  title: string,
  filterRule: Partial<StreamOutputFilterRule>,
  onCancel: () => void,
  handleSubmit: (values: Partial<StreamOutputFilterRule>) => void,
  destinationType: string,
};

const FilterRuleForm = ({ title, filterRule, onCancel, handleSubmit, destinationType }: Props) => (
  <BootstrapModalWrapper showModal
                         bsSize="lg"
                         role="alertdialog"
                         onHide={onCancel}>
    <Formik<StreamOutputFilterRuleValues> initialValues={{ ...filterRule, destination_type: destinationType }}
                                          onSubmit={() => {}}
                                          validateOnBlur={false}
                                          validateOnMount>
      {({ isSubmitting, values }) => (
        <>
          <Modal.Header closeButton>
            <Modal.Title>{title}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <FormikInput id="title"
                         name="title"
                         label="Title"
                         help="Rule title"
                         required />
            <FormikInput id="description"
                         name="description"
                         label="Description"
                         help="Rule description"
                         required />
            <FormikInput id="enabled"
                         name="enabled"
                         type="checkbox"
                         label="Enable rule" />
            <label htmlFor="rule_builder">Rule Builder</label>
            <FilterRulesFields type="condition" />
            <Modal.Footer>
              <ModalSubmit isSubmitting={isSubmitting}
                           isAsyncSubmit
                           onSubmit={() => { handleSubmit(values); }}
                           onCancel={onCancel}
                           submitButtonText={values?.id ? 'Update' : 'Create'}
                           submitLoadingText={values?.id ? 'Updating filter' : 'Saving filter'} />
            </Modal.Footer>
          </Modal.Body>
        </>
      )}
    </Formik>
  </BootstrapModalWrapper>
);

export default FilterRuleForm;
