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
import { useFormikContext } from 'formik';
import * as React from 'react';
import { useCallback } from 'react';

/**
 * Renders a html form, which can be used inside other forms.
 * Please note that nested forms are not valid DOM and should be avoided if possible.
 * Consider displaying the inner form in a portal instead.
 */

const NestedForm = ({ children }: React.PropsWithChildren) => {
  const { handleSubmit, handleReset } = useFormikContext();
  const onSubmit = useCallback((e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    e.stopPropagation();
    handleSubmit(e);
  }, [handleSubmit]);

  const onReset = useCallback((e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    e.stopPropagation();
    handleReset(e);
  }, [handleReset]);

  return (
    <form onSubmitCapture={onSubmit}
          onResetCapture={onReset}>
      {children}
    </form>
  );
};

export default NestedForm;
