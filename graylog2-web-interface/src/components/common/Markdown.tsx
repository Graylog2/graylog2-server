import * as React from 'react';
import { useMemo } from 'react';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

type Props = {
  text: string,
}

const Markdown = ({ text }: Props) => {
  const markdown = useMemo(() => DOMPurify.sanitize(marked(text ?? '')), [text]);

  // eslint-disable-next-line react/no-danger
  return <div dangerouslySetInnerHTML={{ __html: markdown }} />;
};

export default Markdown;
