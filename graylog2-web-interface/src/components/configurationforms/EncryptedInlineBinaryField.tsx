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
import React, { useState } from 'react';
import styled from 'styled-components';

import { Button, Input } from 'components/bootstrap';
import { optionalMarker } from 'components/configurationforms/FieldHelpers';

import type { EncryptedFieldValue, InlineBinaryField as InlineBinaryFieldType } from './types';

type Props = {
  autoFocus?: boolean
  field: InlineBinaryFieldType,
  dirty?: boolean
  onChange: (title: string, value: any, dirty?: boolean) => void,
  title: string,
  typeName: string,
  value?: EncryptedFieldValue<string>,
};

const FileContent = styled.span`
  vertical-align: middle;
`;

const EncryptedInlineBinaryField = ({ field, title, typeName, dirty = false, onChange, value = {}, autoFocus = false }: Props) => {
  const [fileName, setFileName] = useState(undefined);
  const [isResetted, setIsResetted] = useState<boolean>(false);
  const isValuePresent = value.is_set;
  const isRequired = !field.is_optional;
  const showReadOnly = !dirty && isValuePresent;
  const fieldId = `${typeName}-${title}`;

  const labelContent = <>{field.human_name} {optionalMarker(field)}</>;

  const handleFileRead = (fileReader: FileReader, file) => {
    const dataUrl = fileReader.result;

    if (dataUrl && typeof dataUrl === 'string') {
      const dataString = dataUrl.replace(/^data:[\s\S]+\/[\s\S]+;base64,/, '');
      setFileName(file.name);

      if (field.is_encrypted) {
        onChange(title, { set_value: dataString });
      } else {
        onChange(title, dataString);
      }
    }
  };

  const handleReset = () => {
    setIsResetted(true);
    onChange(title, { delete_value: true });
  };

  const handleUndoReset = () => {
    setIsResetted(false);
    setFileName(undefined);
    onChange(title, { is_set: true }, false);
  };

  const resetButton = () => {
    if (isValuePresent) {
      return (
        <Button type="button" onClick={handleReset}>
          Reset
        </Button>
      );
    }

    return null;
  };

  const undoResetButton = () => {
    if (!isResetted) return null;

    return (
      <Button type="button" onClick={handleUndoReset}>
        Undo Reset
      </Button>
    );
  };

  const removeButton = () => {
    if (fileName) {
      return (
        <Button type="button" onClick={() => { setFileName(undefined); onChange(title, ''); }}>
          Remove
        </Button>
      );
    }

    return null;
  };

  const handleFileUpload = (file: File) => {
    const fileReader = new FileReader();

    fileReader.onloadend = (_) => handleFileRead(fileReader, file);

    if (file) {
      fileReader.readAsDataURL(file);
    }
  };

  const readOnlyFileInput = () => (
    <Input id={fieldId}
           type="password"
           name={`configuration[${title}]`}
           label={labelContent}
           required={isRequired}
           readOnly
           help={field.description}
           value="encrypted value"
           buttonAfter={resetButton()}
           autoFocus={autoFocus} />
  );

  const fileInput = () => (
    (fileName) ? (
      <Input id={fieldId}
             name={`configuration[${title}]`}
             label={labelContent}
             required={isRequired}
             help={field.description}
             autoFocus={autoFocus}
             buttonAfter={<>{removeButton()}{undoResetButton()}</>}>
        <FileContent>{fileName}</FileContent>
      </Input>
    ) : (
      <Input id={fieldId}
             type="file"
             name={`configuration[${title}]`}
             label={labelContent}
             required={isRequired}
             help={field.description}
             buttonAfter={undoResetButton()}
             onChange={(e) => handleFileUpload(e.target.files[0])}
             autoFocus={autoFocus} />
    )
  );

  return (
    showReadOnly ? readOnlyFileInput() : fileInput());
};

export default EncryptedInlineBinaryField;
