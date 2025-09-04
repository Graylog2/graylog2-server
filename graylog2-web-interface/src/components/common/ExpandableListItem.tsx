import * as React from 'react';
import styled, { css } from 'styled-components';

import { Checkbox } from 'components/bootstrap';

import Icon from './Icon';

const ItemWrap = styled.li<{ $padded: boolean }>(
  ({ $padded }) => css`
    padding: ${$padded ? '10px 5px' : ''};
  `,
);

const Container = styled.div(
  ({ theme }) => css`
    display: flex;
    font-size: ${theme.fonts.size.body};
    line-height: 20px;

    label {
      min-height: 20px;
      margin-bottom: 2px;
      margin-right: 5px;

      * {
        cursor: pointer;
      }
    }
  `,
);

const Toggle = styled.div`
  display: inline-block;
  width: 20px;
  margin-right: 5px;
`;

const IconContainer = styled.div(
  ({ theme }) => css`
    cursor: pointer;
    font-size: ${theme.fonts.size.large};
    line-height: 20px;
    width: 1em;
    height: 1em;
    vertical-align: text-top;

    &:hover {
      color: ${theme.colors.variant.primary};
      opacity: 1;
    }
  `,
);

const HeaderWrap = styled.span(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.large};
  `,
);

const Header = styled.button`
  display: flex;
  border: 0;
  padding: 0;
  text-align: left;
  background: transparent;
`;

const Subheader = styled.span(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.body};
    margin-left: 0.5em;
    color: ${theme.colors.gray[70]};
  `,
);

const ExpandableContent = styled.div(
  ({ theme }) => css`
    border-left: 1px ${theme.colors.gray[90]} solid;
    margin-left: 35px;
    margin-top: 10px;
    padding-left: 5px;
  `,
);

const _filterInputProps = (props) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { expanded, indetermined, stayExpanded, padded, ...inputProps } = props;

  return inputProps;
};

type Props = React.ComponentProps<typeof Checkbox> & {
  header: React.ReactNode;
  checked?: boolean;
  indetermined?: boolean;
  selectable?: boolean;
  expandable?: boolean;
  expanded?: boolean;
  stayExpanded?: boolean;
  subheader?: React.ReactNode;
  children?: React.ReactNode;
  padded?: boolean;
  readOnly?: boolean;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
};

const ExpandableListItem = ({
  header,
  checked = false,
  indetermined = false,
  selectable = true,
  expandable = true,
  expanded = false,
  stayExpanded = false,
  subheader,
  children = [],
  padded = true,
  readOnly = false,
  onChange = () => undefined,
  ...otherProps
}: Props) => {
  const [isExpanded, setIsExpanded] = React.useState(expanded);
  const checkboxRef = React.useRef<any>(null);

  React.useEffect(() => {
    if (checkboxRef.current) {
      checkboxRef.current.indeterminate = indetermined;
    }
  }, [indetermined]);

  React.useEffect(() => {
    if (expanded !== isExpanded) {
      if (stayExpanded) {
        setIsExpanded(true);
      } else {
        setIsExpanded(expanded);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [expanded, stayExpanded]);

  const _toggleExpand = React.useCallback(() => {
    if (stayExpanded) {
      setIsExpanded(true);
    } else {
      setIsExpanded((prev) => !prev);
    }
  }, [stayExpanded]);

  const _clickOnHeader = React.useCallback(() => {
    if (checkboxRef.current) {
      checkboxRef.current.click();
    }
  }, []);

  const inputProps = _filterInputProps(otherProps);

  const headerToRender = selectable ? (
    <Header type="button" tabIndex={0} onClick={_clickOnHeader}>
      {header}
    </Header>
  ) : (
    header
  );

  return (
    <ItemWrap $padded={padded}>
      <Container>
        {selectable && (
          <Checkbox
            inputRef={checkboxRef}
            inline
            title="Select item"
            checked={checked}
            readOnly={readOnly}
            onChange={onChange}
            {...inputProps}
          />
        )}
        {expandable && (
          <Toggle
            role="button"
            tabIndex={0}
            onClick={_toggleExpand}
            title={`${isExpanded ? 'Shrink' : 'Expand'} list item`}>
            <IconContainer>
              <Icon name={isExpanded ? 'expand_circle_up' : 'expand_circle_down'} />
            </IconContainer>
          </Toggle>
        )}
        <HeaderWrap className="header">
          {headerToRender}
          {subheader && <Subheader>{subheader}</Subheader>}
        </HeaderWrap>
      </Container>
      <ExpandableContent>{isExpanded && children}</ExpandableContent>
    </ItemWrap>
  );
};

export default ExpandableListItem;
