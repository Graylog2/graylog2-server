import styled from 'styled-components';

const MessageDetailsDefinitionList = styled.dl(({ theme }) => `
  margin-top: 10px;
  margin-bottom: 0;

  dt {
    font-weight: bold;
    margin-left: 1px;
  }

  dd {
    margin-bottom: 5px;
    padding-bottom: 5px;
    margin-left: 1px; /* Ensures that italic text is not cut */

    &.stream-list ul {
      list-style-type: disc;
      padding-left: 25px;

      li {
        margin-top: 3px;
      }
    }

    div.message-field-actions {
      padding-left: 10px;
      position: relative;
      top: -10px;
    }
  }

  &.message-details-fields span:not(:last-child) dd {
    border-bottom: 1px solid ${theme.color.gray[90]};
  }

  &.message-details-fields dd {
    white-space: pre-wrap;
  }

  &.message-details-fields .field-value {
    font-family: monospace;
  }

  &.message-details-fields dd.message-field .field-value {
    max-height: 500px;
    overflow: auto;
  }
`);

export default MessageDetailsDefinitionList;
