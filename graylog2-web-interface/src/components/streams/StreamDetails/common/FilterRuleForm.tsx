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
import { FormikInput } from 'components/common';

import type { StreamOutputFilterRule } from './Types';
import FilterRulesFields from './FilterRulesFields';

type StreamOutputFilterRuleValues = Partial<StreamOutputFilterRule>;
type Props ={
  title: string,
  filterRule: StreamOutputFilterRule,
  onCancel: () => void,
};

const FilterRuleForm = ({ title, filterRule, onCancel }: Props) => (
  <BootstrapModalWrapper showModal
                         bsSize="lg"
                         role="alertdialog"
                         onHide={onCancel}>
    <Formik<StreamOutputFilterRuleValues> initialValues={filterRule}
                                          onSubmit={() => {}}
                                          validateOnBlur={false}
                                          validateOnMount>
      {() => (
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
            <FormikInput id="enabled"
                         name="enabled"
                         type="checkbox"
                         label="Enable rule" />
            <label htmlFor="rule_builder">Rule Builder</label>
            <FilterRulesFields type="condition" />
          </Modal.Body>
        </>
      )}
    </Formik>
  </BootstrapModalWrapper>
);

export default FilterRuleForm;
