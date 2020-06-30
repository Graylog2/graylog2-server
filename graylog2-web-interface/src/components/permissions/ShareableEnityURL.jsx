// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { ClipboardButton, Icon } from 'components/common';
import { Alert, FormGroup, InputGroup, FormControl } from 'components/graylog';
import { type ThemeInterface } from 'theme';

const Container: StyledComponent<{}, ThemeInterface, Alert> = styled(Alert)`
  display: flex;
  margin-top: 20px;
`;

const VerticalCenter = styled.div`
  height: 34px;
  display: flex;
  align-items: center;
`;

const URLColumn = styled.div`
  margin-left: 10px;
  flex: 1;
`;

const StyledFormControl: StyledComponent<{}, ThemeInterface, FormControl> = styled(FormControl)(({ theme }) => `
  &[readonly] {
    background-color: ${theme.colors.global.inputBackground};
  }
`);

const InputGroupAddon = styled(InputGroup.Addon)(() => `
  padding: 0;
`);

const StyledClipboardButton = styled(ClipboardButton)`
  border-radius: 0;
  border: 0;
`;

type Props = {
  entityURL?: string,
};

const ShareableEnityURL = ({ entityURL }: Props) => (
  <Container>
    <VerticalCenter>
      <b>Sharable URL:</b>
    </VerticalCenter>
    <URLColumn>
      <FormGroup>
        <InputGroup>
          <StyledFormControl type="text" value={entityURL} readOnly />
          <InputGroupAddon>
            <StyledClipboardButton text={entityURL} title={<Icon name="copy" fixedWidth />} />
          </InputGroupAddon>
        </InputGroup>
      </FormGroup>
      <div>
        You or anyone authorized to view can access this link.
      </div>
    </URLColumn>
  </Container>
);

ShareableEnityURL.defaultProps = {
  entityURL: window.location.href,
};
export default ShareableEnityURL;
