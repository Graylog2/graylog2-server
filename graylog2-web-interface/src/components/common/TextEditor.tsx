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
import { RichTextEditor } from '@mantine/tiptap';
import { useEditor, BubbleMenu } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';

type Props = {
  onChange: (value: string) => void,
  customControl?: boolean;
  customControlOnClick?: () => void,
  customControlItem?: React.ReactNode,
};

/**
 * This component is used for the event procedures feature and renders different
 * text controls such as bold and underline. It also supports custom controls.
 */
const TextEditor = ({ onChange, customControl = false, customControlOnClick = undefined, customControlItem, ...props }: Props) => {
  const editor = useEditor({
    extensions: [
      StarterKit,
    ],
    onUpdate({ editor }) {
      onChange(editor.getHTML());
    },
  });

  const renderCustomControl = () => {
    return (
      <RichTextEditor.Control onClick={customControlOnClick}>
        {customControlItem}
      </RichTextEditor.Control>
    );
  };

  return (
    <RichTextEditor editor={editor}>
      <RichTextEditor.Toolbar sticky stickyOffset={60}>
        <RichTextEditor.ControlsGroup>
          <RichTextEditor.Bold />
          <RichTextEditor.Italic />
          <RichTextEditor.Underline />
          <RichTextEditor.Strikethrough />
        </RichTextEditor.ControlsGroup>

        <RichTextEditor.ControlsGroup>
          <RichTextEditor.H1 />
          <RichTextEditor.H2 />
        </RichTextEditor.ControlsGroup>

        <RichTextEditor.ControlsGroup>
          <RichTextEditor.Blockquote />
          <RichTextEditor.BulletList />
          <RichTextEditor.OrderedList />
        </RichTextEditor.ControlsGroup>

        <RichTextEditor.ControlsGroup>
          <RichTextEditor.AlignLeft />
          <RichTextEditor.AlignCenter />
        </RichTextEditor.ControlsGroup>

        <RichTextEditor.ControlsGroup>
          <RichTextEditor.Undo />
          <RichTextEditor.Redo />
        </RichTextEditor.ControlsGroup>

        <RichTextEditor.ControlsGroup>
          {customControl && renderCustomControl()}
        </RichTextEditor.ControlsGroup>
      </RichTextEditor.Toolbar>

      {editor && (
        <BubbleMenu editor={editor}>
          <RichTextEditor.ControlsGroup>
            <RichTextEditor.Bold />
            <RichTextEditor.Italic />
            <RichTextEditor.Link />
          </RichTextEditor.ControlsGroup>
        </BubbleMenu>
      )}

      <RichTextEditor.Content />
    </RichTextEditor>
  );
};

export default TextEditor;
