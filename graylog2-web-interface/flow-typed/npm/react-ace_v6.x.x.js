// flow-typed signature: d3ac32b6e77a4e39e495305db82340b7
// flow-typed version: bdb93522e7/react-ace_v6.x.x/flow_>=v0.64.x

// This file was created by hand using the following resources:

// 1. https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/ace/index.d.ts
// 2. https://github.com/thlorenz/brace/blob/master/index.d.ts
// 3. https://github.com/securingsincity/react-ace/blob/master/types.d.ts

// Consider using the listed resources to improve these definitions.

declare module "react-ace" {
  declare export type Annotation = {
      row: number,
      column: number,
      type: string,
      text: string,
  };

  declare export type Delta = {
      action: string,
      range: Range,
      text: string,
      lines: string[],
  };

  declare export type Document = {
      on(event: string, fn: (e: any) => any): void,

      /**
       * Replaces all the lines in the current `Document` with the value of `text`.
       * @param text The text to use
       **/
      setValue(text: string): void,

      /**
       * Returns all the lines in the document as a single string, split by the new line character.
       **/
      getValue(): string,

      /**
       * Creates a new `Anchor` to define a floating point in the document.
       * @param row The row number to use
       * @param column The column number to use
       **/
      createAnchor(row: number, column: number): void,

      /**
       * Returns the newline character that's being used, depending on the value of `newLineMode`.
       **/
      getNewLineCharacter(): string,

      /**
       * [Sets the new line mode.]{: #Document.setNewLineMode.desc}
       * @param newLineMode [The newline mode to use, can be either `windows`, `unix`, or `auto`]{: #Document.setNewLineMode.param}
       **/
      setNewLineMode(newLineMode: string): void,

      /**
       * [Returns the type of newlines being used, either `windows`, `unix`, or `auto`]{: #Document.getNewLineMode}
       **/
      getNewLineMode(): string,

      /**
       * Returns `true` if `text` is a newline character (either `\r\n`, `\r`, or `\n`).
       * @param text The text to check
       **/
      isNewLine(text: string): boolean,

      /**
       * Returns a verbatim copy of the given line as it is in the document
       * @param row The row index to retrieve
       **/
      getLine(row: number): string,

      /**
       * Returns an array of strings of the rows between `firstRow` and `lastRow`. This function is inclusive of `lastRow`.
       * @param firstRow The first row index to retrieve
       * @param lastRow The final row index to retrieve
       **/
      getLines(firstRow: number, lastRow: number): string[],

      /**
       * Returns all lines in the document as string array. Warning: The caller should not modify this array!
       **/
      getAllLines(): string[],

      /**
       * Returns the number of rows in the document.
       **/
      getLength(): number,

      /**
       * [Given a range within the document, this function returns all the text within that range as a single string.]{: #Document.getTextRange.desc}
       * @param range The range to work with
       **/
      getTextRange(range: Range): string,

      /**
       * Inserts a block of `text` and the indicated `position`.
       * @param position The position to start inserting at
       * @param text A chunk of text to insert
       **/
      insert(position: Position, text: string): any,

      /**
       * Inserts the elements in `lines` into the document, starting at the row index given by `row`. This method also triggers the `'change'` event.
       * @param row The index of the row to insert at
       * @param lines An array of strings
       **/
      insertLines(row: number, lines: string[]): any,

      /**
       * Inserts a new line into the document at the current row's `position`. This method also triggers the `'change'` event.
       * @param position The position to insert at
       **/
      insertNewLine(position: Position): any,

      /**
       * Inserts `text` into the `position` at the current row. This method also triggers the `'change'` event.
       * @param position The position to insert at
       * @param text A chunk of text
       **/
      insertInLine(position: any, text: string): any,

      /**
       * Removes the `range` from the document.
       * @param range A specified Range to remove
       **/
      remove(range: Range): any,

      /**
       * Removes the specified columns from the `row`. This method also triggers the `'change'` event.
       * @param row The row to remove from
       * @param startColumn The column to start removing at
       * @param endColumn The column to stop removing at
       **/
      removeInLine(row: number, startColumn: number, endColumn: number): any,

      /**
       * Removes a range of full lines. This method also triggers the `'change'` event.
       * @param firstRow The first row to be removed
       * @param lastRow The last row to be removed
       **/
      removeLines(firstRow: number, lastRow: number): string[],

      /**
       * Removes the new line between `row` and the row immediately following it. This method also triggers the `'change'` event.
       * @param row The row to check
       **/
      removeNewLine(row: number): void,

      /**
       * Replaces a range in the document with the new `text`.
       * @param range A specified Range to replace
       * @param text The new text to use as a replacement
       **/
      replace(range: Range, text: string): any,

      /**
       * Applies all the changes previously accumulated. These can be either `'includeText'`, `'insertLines'`, `'removeText'`, and `'removeLines'`.
       **/
      applyDeltas(deltas: Delta[]): void,

      /**
       * Reverts any changes previously applied. These can be either `'includeText'`, `'insertLines'`, `'removeText'`, and `'removeLines'`.
       **/
      revertDeltas(deltas: Delta[]): void,

      /**
       * Converts an index position in a document to a `{row, column}` object.
       * Index refers to the "absolute position" of a character in the document. For example:
       * ```javascript
       * var x = 0, // 10 characters, plus one for newline
       * var y = -1,
       * ```
       * Here, `y` is an index 15: 11 characters for the first row, and 5 characters until `y` in the second.
       * @param index An index to convert
       * @param startRow=0 The row from which to start the conversion
       **/
      indexToPosition(index: number, startRow: number): Position,

      /**
       * Converts the `{row, column}` position in a document to the character's index.
       * Index refers to the "absolute position" of a character in the document. For example:
       * ```javascript
       * var x = 0, // 10 characters, plus one for newline
       * var y = -1,
       * ```
       * Here, `y` is an index 15: 11 characters for the first row, and 5 characters until `y` in the second.
       * @param pos The `{row, column}` to convert
       * @param startRow=0 The row from which to start the conversion
       **/
      positionToIndex(pos: Position, startRow: number): number,
  };

  declare export type TextMode = {
      getTokenizer(): any,

      toggleCommentLines(
          state: any,
          doc: any,
          startRow: any,
          endRow: any
      ): void,

      getNextLineIndent(state: any, line: any, tab: any): string,

      checkOutdent(state: any, line: any, input: any): boolean,

      autoOutdent(state: any, doc: any, row: any): void,

      createWorker(session: any): any,

      createModeDelegates(mapping: any): void,

      transformAction(
          state: any,
          action: any,
          editor: any,
          session: any,
          param: any
      ): any,
  };

  declare export type UndoManager = {
      /**
       * Provides a means for implementing your own undo manager. `options` has one property, `args`, an [[Array `Array`]], with two elements:
       * - `args[0]` is an array of deltas
       * - `args[1]` is the document to associate with
       * @param options Contains additional properties
       **/
      execute(options: any): void,

      /**
       * [Perform an undo operation on the document, reverting the last change.]{: #UndoManager.undo}
       * @param dontSelect {:dontSelect}
       **/
      undo(dontSelect?: boolean): Range,

      /**
       * [Perform a redo operation on the document, reimplementing the last change.]{: #UndoManager.redo}
       * @param dontSelect {:dontSelect}
       **/
      redo(dontSelect: boolean): void,

      /**
       * Destroys the stack of undo and redo redo operations.
       **/
      reset(): void,

      /**
       * Returns `true` if there are undo operations left to perform.
       **/
      hasUndo(): boolean,

      /**
       * Returns `true` if there are redo operations left to perform.
       **/
      hasRedo(): boolean,

      /**
       * Returns `true` if the dirty counter is 0
       **/
      isClean(): boolean,

      /**
       * Sets dirty counter to 0
       **/
      markClean(): void,
  };

  declare export type Marker = {
      startRow: number,
      startCol: number,
      endRow: number,
      endCol: number,
      className: string,
      type: string,
  };

  declare export type TokenInfo = {
      value: string,
  };

  declare export type Tokenizer = {
      /**
       * Returns an object containing two properties: `tokens`, which contains all the tokens; and `state`, the current state.
       **/
      getLineTokens(): any,
  };

  declare export type BackgroundTokenizer = {
      states: any[],

      /**
       * Sets a new tokenizer for this object.
       * @param tokenizer The new tokenizer to use
       **/
      setTokenizer(tokenizer: Tokenizer): void,

      /**
       * Sets a new document to associate with this object.
       * @param doc The new document to associate with
       **/
      setDocument(doc: Document): void,

      /**
       * Emits the `'update'` event. `firstRow` and `lastRow` are used to define the boundaries of the region to be updated.
       * @param firstRow The starting row region
       * @param lastRow The final row region
       **/
      fireUpdateEvent(firstRow: number, lastRow: number): void,

      /**
       * Starts tokenizing at the row indicated.
       * @param startRow The row to start at
       **/
      start(startRow: number): void,

      /**
       * Stops tokenizing.
       **/
      stop(): void,

      /**
       * Gives list of tokens of the row. (tokens are cached)
       * @param row The row to get tokens at
       **/
      getTokens(row: number): TokenInfo[],

      /**
       * [Returns the state of tokenization at the end of a row.]{: #BackgroundTokenizer.getState}
       * @param row The row to get state at
       **/
      getState(row: number): string,
  };

  declare export type Range = {
      startRow: number,

      startColumn: number,

      endRow: number,

      endColumn: number,

      start: Position,

      end: Position,

      isEmpty(): boolean,

      /**
       * Returns `true` if and only if the starting row and column, and ending row and column, are equivalent to those given by `range`.
       * @param range A range to check against
       **/
      isEqual(range: Range): void,

      /**
       * Returns a string containing the range's row and column information, given like this:
       * ```
       * [start.row/start.column] -> [end.row/end.column]
       * ```
       **/
      toString(): void,

      /**
       * Returns `true` if the `row` and `column` provided are within the given range. This can better be expressed as returning `true` if:
       * ```javascript
       * this.start.row <= row <= this.end.row &&
       * this.start.column <= column <= this.end.column
       * ```
       * @param row A row to check for
       * @param column A column to check for
       **/
      contains(row: number, column: number): boolean,

      /**
       * Compares `this` range (A) with another range (B).
       * @param range A range to compare with
       **/
      compareRange(range: Range): number,

      /**
       * Checks the row and column points of `p` with the row and column points of the calling range.
       * @param p A point to compare with
       **/
      comparePoint(p: Range): number,

      /**
       * Checks the start and end points of `range` and compares them to the calling range. Returns `true` if the `range` is contained within the caller's range.
       * @param range A range to compare with
       **/
      containsRange(range: Range): boolean,

      /**
       * Returns `true` if passed in `range` intersects with the one calling this method.
       * @param range A range to compare with
       **/
      intersects(range: Range): boolean,

      /**
       * Returns `true` if the caller's ending row point is the same as `row`, and if the caller's ending column is the same as `column`.
       * @param row A row point to compare with
       * @param column A column point to compare with
       **/
      isEnd(row: number, column: number): boolean,

      /**
       * Returns `true` if the caller's starting row point is the same as `row`, and if the caller's starting column is the same as `column`.
       * @param row A row point to compare with
       * @param column A column point to compare with
       **/
      isStart(row: number, column: number): boolean,

      /**
       * Sets the starting row and column for the range.
       * @param row A row point to set
       * @param column A column point to set
       **/
      setStart(row: number, column: number): void,

      /**
       * Sets the starting row and column for the range.
       * @param row A row point to set
       * @param column A column point to set
       **/
      setEnd(row: number, column: number): void,

      /**
       * Returns `true` if the `row` and `column` are within the given range.
       * @param row A row point to compare with
       * @param column A column point to compare with
       **/
      inside(row: number, column: number): boolean,

      /**
       * Returns `true` if the `row` and `column` are within the given range's starting points.
       * @param row A row point to compare with
       * @param column A column point to compare with
       **/
      insideStart(row: number, column: number): boolean,

      /**
       * Returns `true` if the `row` and `column` are within the given range's ending points.
       * @param row A row point to compare with
       * @param column A column point to compare with
       **/
      insideEnd(row: number, column: number): boolean,

      /**
       * Checks the row and column points with the row and column points of the calling range.
       * @param row A row point to compare with
       * @param column A column point to compare with
       **/
      compare(row: number, column: number): number,

      /**
       * Checks the row and column points with the row and column points of the calling range.
       * @param row A row point to compare with
       * @param column A column point to compare with
       **/
      compareStart(row: number, column: number): number,

      /**
       * Checks the row and column points with the row and column points of the calling range.
       * @param row A row point to compare with
       * @param column A column point to compare with
       **/
      compareEnd(row: number, column: number): number,

      /**
       * Checks the row and column points with the row and column points of the calling range.
       * @param row A row point to compare with
       * @param column A column point to compare with
       **/
      compareInside(row: number, column: number): number,

      /**
       * Returns the part of the current `Range` that occurs within the boundaries of `firstRow` and `lastRow` as a new `Range` object.
       * @param firstRow The starting row
       * @param lastRow The ending row
       **/
      clipRows(firstRow: number, lastRow: number): Range,

      /**
       * Changes the row and column points for the calling range for both the starting and ending points.
       * @param row A new row to extend to
       * @param column A new column to extend to
       **/
      extend(row: number, column: number): Range,

      /**
       * Returns `true` if the range spans across multiple lines.
       **/
      isMultiLine(): boolean,

      /**
       * Returns a duplicate of the calling range.
       **/
      clone(): Range,

      /**
       * Returns a range containing the starting and ending rows of the original range, but with a column value of `0`.
       **/
      collapseRows(): Range,

      /**
       * Given the current `Range`, this function converts those starting and ending points into screen positions, and then returns a new `Range` object.
       * @param session The `EditSession` to retrieve coordinates from
       **/
      toScreenRange(session: IEditSession): Range,

      /**
       * Creates and returns a new `Range` based on the row and column of the given parameters.
       * @param start A starting point to use
       * @param end An ending point to use
       **/
      fromPoints(start: Range, end: Range): Range,
  };

  declare export type CommandBindKey = {
      win: string,
      mac: string,
  };

  declare export type Command = {
      name: string,
      bindKey: CommandBindKey,

      exec(): any,
  };

  declare export type AceOptions = {
      selectionStyle?: "line" | "text",
      highlightActiveLine?: boolean,
      highlightSelectedWord?: boolean,
      readOnly?: boolean,
      cursorStyle?: "ace" | "slim" | "smooth" | "wide",
      mergeUndoDeltas?: false | true | "always",
      behavioursEnabled?: boolean,
      wrapBehavioursEnabled?: boolean,

      /**
       * this is needed if editor is inside scrollable page
       */
      autoScrollEditorIntoView?: boolean,
      hScrollBarAlwaysVisible?: boolean,
      vScrollBarAlwaysVisible?: boolean,
      highlightGutterLine?: boolean,
      animatedScroll?: boolean,
      showInvisibles?: boolean,
      showPrintMargin?: boolean,
      printMarginColumn?: boolean,
      printMargin?: boolean,
      fadeFoldWidgets?: boolean,
      showFoldWidgets?: boolean,
      showLineNumbers?: boolean,
      showGutter?: boolean,
      displayIndentGuides?: boolean,

      /**
       * number or css font-size string
       */
      fontSize?: number | string,

      /**
       * css
       */
      fontFamily?: string,
      maxLines?: number,
      minLines?: number,
      scrollPastEnd?: boolean,
      fixedWidthGutter?: boolean,

      /**
       * path to a theme e.g "ace/theme/textmate"
       */
      theme?: string,
      scrollSpeed?: number,
      dragDelay?: number,
      dragEnabled?: boolean,
      focusTimout?: number,
      tooltipFollowsMouse?: boolean,
      firstLineNumber?: number,
      overwrite?: boolean,
      newLineMode?: boolean,
      useWorker?: boolean,
      useSoftTabs?: boolean,
      tabSize?: number,
      wrap?: boolean,
      foldStyle?: boolean,

      /**
       * path to a mode e.g "ace/mode/text"
       */
      mode?: string,

      /**
       * on by default
       */
      enableMultiselect?: boolean,
      enableEmmet?: boolean,
      enableBasicAutocompletion?: boolean,
      enableLiveAutocompletion?: boolean,
      enableSnippets?: boolean,
      spellcheck?: boolean,
      useElasticTabstops?: boolean,
  };

  declare export type EditorProps = {
      $blockScrolling?: number | boolean,
      $blockSelectEnabled?: boolean,
      $enableBlockSelect?: boolean,
      $enableMultiselect?: boolean,
      $highlightPending?: boolean,
      $highlightTagPending?: boolean,
      $multiselectOnSessionChange?: (...args: any[]) => any,
      $onAddRange?: (...args: any[]) => any,
      $onChangeAnnotation?: (...args: any[]) => any,
      $onChangeBackMarker?: (...args: any[]) => any,
      $onChangeBreakpoint?: (...args: any[]) => any,
      $onChangeFold?: (...args: any[]) => any,
      $onChangeFrontMarker?: (...args: any[]) => any,
      $onChangeMode?: (...args: any[]) => any,
      $onChangeTabSize?: (...args: any[]) => any,
      $onChangeWrapLimit?: (...args: any[]) => any,
      $onChangeWrapMode?: (...args: any[]) => any,
      $onCursorChange?: (...args: any[]) => any,
      $onDocumentChange?: (...args: any[]) => any,
      $onMultiSelect?: (...args: any[]) => any,
      $onRemoveRange?: (...args: any[]) => any,
      $onScrollLeftChange?: (...args: any[]) => any,
      $onScrollTopChange?: (...args: any[]) => any,
      $onSelectionChange?: (...args: any[]) => any,
      $onSingleSelect?: (...args: any[]) => any,
      $onTokenizerUpdate?: (...args: any[]) => any,
  };

  declare export type Props = {
      name?: string,

      /**
       * For available modes see https://github.com/thlorenz/brace/tree/master/mode
       */
      mode?: string,

      /**
       * For available themes see https://github.com/thlorenz/brace/tree/master/theme
       */
      theme?: string,
      height?: string,
      width?: string,
      className?: string,
      fontSize?: number,
      showGutter?: boolean,
      showPrintMargin?: boolean,
      highlightActiveLine?: boolean,
      focus?: boolean,
      cursorStart?: number,
      wrapEnabled?: boolean,
      readOnly?: boolean,
      minLines?: number,
      maxLines?: number,
      enableBasicAutocompletion?: boolean,
      enableLiveAutocompletion?: boolean,
      tabSize?: number,
      value?: string,
      defaultValue?: string,
      scrollMargin?: number[],
      onLoad?: (editor: EditorProps) => void,
      onBeforeLoad?: (ace: any) => void,
      onChange?: (value: string, event?: any) => void,
      onSelection?: (selectedText: string, event?: any) => void,
      onCopy?: (value: string) => void,
      onPaste?: (value: string) => void,
      onFocus?: (event: any) => void,
      onBlur?: (event: any) => void,
      onValidate?: (annotations: Array<Annotation>) => void,
      onScroll?: (editor: EditorProps) => void,
      editorProps?: EditorProps,
      setOptions?: AceOptions,
      keyboardHandler?: string,
      commands?: Array<Command>,
      annotations?: Array<Annotation>,
      markers?: Array<Marker>,
  };

  declare export type IEditSession = {
      selection: AceSelection,

      bgTokenizer: BackgroundTokenizer,

      doc: Document,

      on(event: string, fn: (e: any) => any): void,

      findMatchingBracket(position: Position): void,

      addFold(text: string, range: Range): void,

      getFoldAt(row: number, column: number): any,

      removeFold(arg: any): void,

      expandFold(arg: any): void,

      foldAll(startRow?: number, endRow?: number, depth?: number): void,

      unfold(arg1: any, arg2: boolean): void,

      screenToDocumentColumn(row: number, column: number): void,

      getFoldDisplayLine(
          foldLine: any,
          docRow: number,
          docColumn: number
      ): any,

      getFoldsInRange(range: Range): any,

      highlight(text: string): void,

      /**
       * Sets the `EditSession` to point to a new `Document`. If a `BackgroundTokenizer` exists, it also points to `doc`.
       * @param doc The new `Document` to use
       **/
      setDocument(doc: Document): void,

      /**
       * Returns the `Document` associated with this session.
       **/
      getDocument(): Document,

      /**
       * undefined
       * @param row The row to work with
       **/
      $resetRowCache(row: number): void,

      /**
       * Sets the session text.
       * @param text The new text to place
       **/
      setValue(text: string): void,

      setMode(mode: string): void,

      /**
       * Returns the current [[Document `Document`]] as a string.
       **/
      getValue(): string,

      /**
       * Returns the string of the current selection.
       **/
      getSelection(): AceSelection,

      /**
       * {:BackgroundTokenizer.getState}
       * @param row The row to start at
       **/
      getState(row: number): string,

      /**
       * Starts tokenizing at the row indicated. Returns a list of objects of the tokenized rows.
       * @param row The row to start at
       **/
      getTokens(row: number): TokenInfo[],

      /**
       * Returns an object indicating the token at the current row. The object has two properties: `index` and `start`.
       * @param row The row number to retrieve from
       * @param column The column number to retrieve from
       **/
      getTokenAt(row: number, column: number): TokenInfo,

      /**
       * Sets the undo manager.
       * @param undoManager The new undo manager
       **/
      setUndoManager(undoManager: UndoManager): void,

      /**
       * Returns the current undo manager.
       **/
      getUndoManager(): UndoManager,

      /**
       * Returns the current value for tabs. If the user is using soft tabs, this will be a series of spaces (defined by [[EditSession.getTabSize `getTabSize()`]]): void, otherwise it's simply `'\t'`.
       **/
      getTabString(): string,

      /**
       * Pass `true` to enable the use of soft tabs. Soft tabs means you're using spaces instead of the tab character (`'\t'`).
       * @param useSoftTabs Value indicating whether or not to use soft tabs
       **/
      setUseSoftTabs(useSoftTabs: boolean): void,

      /**
       * Returns `true` if soft tabs are being used, `false` otherwise.
       **/
      getUseSoftTabs(): boolean,

      /**
       * Set the number of spaces that define a soft tab, for example, passing in `4` transforms the soft tabs to be equivalent to four spaces. This function also emits the `changeTabSize` event.
       * @param tabSize The new tab size
       **/
      setTabSize(tabSize: number): void,

      /**
       * Returns the current tab size.
       **/
      getTabSize(): number,

      /**
       * Returns `true` if the character at the position is a soft tab.
       * @param position The position to check
       **/
      isTabStop(position: any): boolean,

      /**
       * Pass in `true` to enable overwrites in your session, or `false` to disable.
       * If overwrites is enabled, any text you enter will type over any text after it. If the value of `overwrite` changes, this function also emites the `changeOverwrite` event.
       * @param overwrite Defines wheter or not to set overwrites
       **/
      setOverwrite(overwrite: boolean): void,

      /**
       * Returns `true` if overwrites are enabled, `false` otherwise.
       **/
      getOverwrite(): boolean,

      /**
       * Sets the value of overwrite to the opposite of whatever it currently is.
       **/
      toggleOverwrite(): void,

      /**
       * Adds `className` to the `row`, to be used for CSS stylings and whatnot.
       * @param row The row number
       * @param className The class to add
       **/
      addGutterDecoration(row: number, className: string): void,

      /**
       * Removes `className` from the `row`.
       * @param row The row number
       * @param className The class to add
       **/
      removeGutterDecoration(row: number, className: string): void,

      /**
       * Returns an array of numbers, indicating which rows have breakpoints.
       **/
      getBreakpoints(): number[],

      /**
       * Sets a breakpoint on every row number given by `rows`. This function also emites the `'changeBreakpoint'` event.
       * @param rows An array of row indices
       **/
      setBreakpoints(rows: any[]): void,

      /**
       * Removes all breakpoints on the rows. This function also emites the `'changeBreakpoint'` event.
       **/
      clearBreakpoints(): void,

      /**
       * Sets a breakpoint on the row number given by `rows`. This function also emites the `'changeBreakpoint'` event.
       * @param row A row index
       * @param className Class of the breakpoint
       **/
      setBreakpoint(row: number, className: string): void,

      /**
       * Removes a breakpoint on the row number given by `rows`. This function also emites the `'changeBreakpoint'` event.
       * @param row A row index
       **/
      clearBreakpoint(row: number): void,

      /**
       * Adds a new marker to the given `Range`. If `inFront` is `true`, a front marker is defined, and the `'changeFrontMarker'` event fires, otherwise, the `'changeBackMarker'` event fires.
       * @param range Define the range of the marker
       * @param clazz Set the CSS class for the marker
       * @param type Identify the type of the marker
       * @param inFront Set to `true` to establish a front marker
       **/
      addMarker(
          range: Range,
          clazz: string,
          type: Function,
          inFront?: boolean
      ): number,

      /**
       * Adds a new marker to the given `Range`. If `inFront` is `true`, a front marker is defined, and the `'changeFrontMarker'` event fires, otherwise, the `'changeBackMarker'` event fires.
       * @param range Define the range of the marker
       * @param clazz Set the CSS class for the marker
       * @param type Identify the type of the marker
       * @param inFront Set to `true` to establish a front marker
       **/
      addMarker(
          range: Range,
          clazz: string,
          type: string,
          inFront?: boolean
      ): number,

      /**
       * Adds a dynamic marker to the session.
       * @param marker object with update method
       * @param inFront Set to `true` to establish a front marker
       **/
      addDynamicMarker(marker: any, inFront: boolean): void,

      /**
       * Removes the marker with the specified ID. If this marker was in front, the `'changeFrontMarker'` event is emitted. If the marker was in the back, the `'changeBackMarker'` event is emitted.
       * @param markerId A number representing a marker
       **/
      removeMarker(markerId: number): void,

      /**
       * Returns an array containing the IDs of all the markers, either front or back.
       * @param inFront If `true`, indicates you only want front markers, `false` indicates only back markers
       **/
      getMarkers(inFront: boolean): any[],

      /**
       * Sets annotations for the `EditSession`. This functions emits the `'changeAnnotation'` event.
       * @param annotations A list of annotations
       **/
      setAnnotations(annotations: Annotation[]): void,

      /**
       * Returns the annotations for the `EditSession`.
       **/
      getAnnotations(): any,

      /**
       * Clears all the annotations for this session. This function also triggers the `'changeAnnotation'` event.
       **/
      clearAnnotations(): void,

      /**
       * If `text` contains either the newline (`\n`) or carriage-return ('\r') characters, `$autoNewLine` stores that value.
       * @param text A block of text
       **/
      $detectNewLine(text: string): void,

      /**
       * Given a starting row and column, this method returns the `Range` of the first word boundary it finds.
       * @param row The row to start at
       * @param column The column to start at
       **/
      getWordRange(row: number, column: number): Range,

      /**
       * Gets the range of a word, including its right whitespace.
       * @param row The row number to start from
       * @param column The column number to start from
       **/
      getAWordRange(row: number, column: number): any,

      /**
       * {:Document.setNewLineMode.desc}
       * @param newLineMode {:Document.setNewLineMode.param}
       **/
      setNewLineMode(newLineMode: string): void,

      /**
       * Returns the current new line mode.
       **/
      getNewLineMode(): string,

      /**
       * Identifies if you want to use a worker for the `EditSession`.
       * @param useWorker Set to `true` to use a worker
       **/
      setUseWorker(useWorker: boolean): void,

      /**
       * Returns `true` if workers are being used.
       **/
      getUseWorker(): boolean,

      /**
       * Reloads all the tokens on the current session. This function calls [[BackgroundTokenizer.start `BackgroundTokenizer.start ()`]] to all the rows, it also emits the `'tokenizerUpdate'` event.
       **/
      onReloadTokenizer(): void,

      /**
       * Sets a new text mode for the `EditSession`. This method also emits the `'changeMode'` event. If a [[BackgroundTokenizer `BackgroundTokenizer`]] is set, the `'tokenizerUpdate'` event is also emitted.
       * @param mode Set a new text mode
       **/
      $mode(mode: TextMode): void,

      /**
       * Returns the current text mode.
       **/
      getMode(): TextMode,

      /**
       * This function sets the scroll top value. It also emits the `'changeScrollTop'` event.
       * @param scrollTop The new scroll top value
       **/
      setScrollTop(scrollTop: number): void,

      /**
       * [Returns the value of the distance between the top of the editor and the topmost part of the visible content.]{: #EditSession.getScrollTop}
       **/
      getScrollTop(): number,

      /**
       * [Sets the value of the distance between the left of the editor and the leftmost part of the visible content.]{: #EditSession.setScrollLeft}
       **/
      setScrollLeft(): void,

      /**
       * [Returns the value of the distance between the left of the editor and the leftmost part of the visible content.]{: #EditSession.getScrollLeft}
       **/
      getScrollLeft(): number,

      /**
       * Returns the width of the screen.
       **/
      getScreenWidth(): number,

      /**
       * Returns a verbatim copy of the given line as it is in the document
       * @param row The row to retrieve from
       **/
      getLine(row: number): string,

      /**
       * Returns an array of strings of the rows between `firstRow` and `lastRow`. This function is inclusive of `lastRow`.
       * @param firstRow The first row index to retrieve
       * @param lastRow The final row index to retrieve
       **/
      getLines(firstRow: number, lastRow: number): string[],

      /**
       * Returns the number of rows in the document.
       **/
      getLength(): number,

      /**
       * {:Document.getTextRange.desc}
       * @param range The range to work with
       **/
      getTextRange(range: Range): string,

      /**
       * Inserts a block of `text` and the indicated `position`.
       * @param position The position {row, column} to start inserting at
       * @param text A chunk of text to insert
       **/
      insert(position: Position, text: string): any,

      /**
       * Removes the `range` from the document.
       * @param range A specified Range to remove
       **/
      remove(range: Range): any,

      /**
       * Reverts previous changes to your document.
       * @param deltas An array of previous changes
       * @param dontSelect [If `true`, doesn't select the range of where the change occured]{: #dontSelect}
       **/
      undoChanges(deltas: any[], dontSelect: boolean): Range,

      /**
       * Re-implements a previously undone change to your document.
       * @param deltas An array of previous changes
       * @param dontSelect {:dontSelect}
       **/
      redoChanges(deltas: any[], dontSelect: boolean): Range,

      /**
       * Enables or disables highlighting of the range where an undo occured.
       * @param enable If `true`, selects the range of the reinserted change
       **/
      setUndoSelect(enable: boolean): void,

      /**
       * Replaces a range in the document with the new `text`.
       * @param range A specified Range to replace
       * @param text The new text to use as a replacement
       **/
      replace(range: Range, text: string): any,

      /**
       * Moves a range of text from the given range to the given position. `toPosition` is an object that looks like this:
       * ```json
       * { row: newRowLocation, column: newColumnLocation }
       * ```
       * @param fromRange The range of text you want moved within the document
       * @param toPosition The location (row and column) where you want to move the text to
       **/
      moveText(fromRange: Range, toPosition: any): Range,

      /**
       * Indents all the rows, from `startRow` to `endRow` (inclusive), by prefixing each row with the token in `indentString`.
       * If `indentString` contains the `'\t'` character, it's replaced by whatever is defined by [[EditSession.getTabString `getTabString()`]].
       * @param startRow Starting row
       * @param endRow Ending row
       * @param indentString The indent token
       **/
      indentRows(
          startRow: number,
          endRow: number,
          indentString: string
      ): void,

      /**
       * Outdents all the rows defined by the `start` and `end` properties of `range`.
       * @param range A range of rows
       **/
      outdentRows(range: Range): void,

      /**
       * Shifts all the lines in the document up one, starting from `firstRow` and ending at `lastRow`.
       * @param firstRow The starting row to move up
       * @param lastRow The final row to move up
       **/
      moveLinesUp(firstRow: number, lastRow: number): number,

      /**
       * Shifts all the lines in the document down one, starting from `firstRow` and ending at `lastRow`.
       * @param firstRow The starting row to move down
       * @param lastRow The final row to move down
       **/
      moveLinesDown(firstRow: number, lastRow: number): number,

      /**
       * Duplicates all the text between `firstRow` and `lastRow`.
       * @param firstRow The starting row to duplicate
       * @param lastRow The final row to duplicate
       **/
      duplicateLines(firstRow: number, lastRow: number): number,

      /**
       * Sets whether or not line wrapping is enabled. If `useWrapMode` is different than the current value, the `'changeWrapMode'` event is emitted.
       * @param useWrapMode Enable (or disable) wrap mode
       **/
      setUseWrapMode(useWrapMode: boolean): void,

      /**
       * Returns `true` if wrap mode is being used, `false` otherwise.
       **/
      getUseWrapMode(): boolean,

      /**
       * Sets the boundaries of wrap. Either value can be `null` to have an unconstrained wrap, or, they can be the same number to pin the limit. If the wrap limits for `min` or `max` are different, this method also emits the `'changeWrapMode'` event.
       * @param min The minimum wrap value (the left side wrap)
       * @param max The maximum wrap value (the right side wrap)
       **/
      setWrapLimitRange(min: number, max: number): void,

      /**
       * This should generally only be called by the renderer when a resize is detected.
       * @param desiredLimit The new wrap limit
       **/
      adjustWrapLimit(desiredLimit: number): boolean,

      /**
       * Returns the value of wrap limit.
       **/
      getWrapLimit(): number,

      /**
       * Returns an object that defines the minimum and maximum of the wrap limit, it looks something like this:
       * { min: wrapLimitRange_min, max: wrapLimitRange_max }
       **/
      getWrapLimitRange(): any,

      /**
       * Given a string, returns an array of the display characters, including tabs and spaces.
       * @param str The string to check
       * @param offset The value to start at
       **/
      $getDisplayTokens(str: string, offset: number): void,

      /**
       * Calculates the width of the string `str` on the screen while assuming that the string starts at the first column on the screen.
       * @param str The string to calculate the screen width of
       * @param maxScreenColumn
       * @param screenColumn
       **/
      $getStringScreenWidth(
          str: string,
          maxScreenColumn: number,
          screenColumn: number
      ): number[],

      /**
       * Returns number of screenrows in a wrapped line.
       * @param row The row number to check
       **/
      getRowLength(row: number): number,

      /**
       * Returns the position (on screen) for the last character in the provided screen row.
       * @param screenRow The screen row to check
       **/
      getScreenLastRowColumn(screenRow: number): number,

      /**
       * For the given document row and column, this returns the column position of the last screen row.
       * @param docRow
       * @param docColumn
       **/
      getDocumentLastRowColumn(docRow: number, docColumn: number): number,

      /**
       * For the given document row and column, this returns the document position of the last row.
       * @param docRow
       * @param docColumn
       **/
      getDocumentLastRowColumnPosition(
          docRow: number,
          docColumn: number
      ): number,

      /**
       * For the given row, this returns the split data.
       **/
      getRowSplitData(): string,

      /**
       * The distance to the next tab stop at the specified screen column.
       * @param screenColumn The screen column to check
       **/
      getScreenTabSize(screenColumn: number): number,

      /**
       * Converts characters coordinates on the screen to characters coordinates within the document. [This takes into account code folding, word wrap, tab size, and any other visual modifications.]{: #conversionConsiderations}
       * @param screenRow The screen row to check
       * @param screenColumn The screen column to check
       **/
      screenToDocumentPosition(screenRow: number, screenColumn: number): any,

      /**
       * Converts document coordinates to screen coordinates. {:conversionConsiderations}
       * @param docRow The document row to check
       * @param docColumn The document column to check
       **/
      documentToScreenPosition(docRow: number, docColumn: number): any,

      /**
       * For the given document row and column, returns the screen column.
       * @param row
       * @param docColumn
       **/
      documentToScreenColumn(row: number, docColumn: number): number,

      /**
       * For the given document row and column, returns the screen row.
       * @param docRow
       * @param docColumn
       **/
      documentToScreenRow(docRow: number, docColumn: number): void,

      /**
       * Returns the length of the screen.
       **/
      getScreenLength(): number,
  };

  declare export type KeyBinding = {
      setDefaultHandler(kb: any): void,

      setKeyboardHandler(kb: any): void,

      addKeyboardHandler(kb: any, pos: any): void,

      removeKeyboardHandler(kb: any): boolean,

      getKeyboardHandler(): any,

      onCommandKey(e: any, hashId: any, keyCode: any): void,

      onTextInput(text: any): void,
  };

  declare export type EditorCommand = {
      name: string,

      bindKey: any,

      exec: Function,

      readOnly?: boolean,
  };

  declare export type VirtualRenderer = {
      scroller: any,

      characterWidth: number,

      lineHeight: number,

      setScrollMargin(
          top: number,
          bottom: number,
          left: number,
          right: number
      ): void,

      screenToTextCoordinates(left: number, top: number): void,

      /**
       * Associates the renderer with an [[EditSession `EditSession`]].
       **/
      setSession(session: IEditSession): void,

      /**
       * Triggers a partial update of the text, from the range given by the two parameters.
       * @param firstRow The first row to update
       * @param lastRow The last row to update
       **/
      updateLines(firstRow: number, lastRow: number): void,

      /**
       * Triggers a full update of the text, for all the rows.
       **/
      updateText(): void,

      /**
       * Triggers a full update of all the layers, for all the rows.
       * @param force If `true`, forces the changes through
       **/
      updateFull(force: boolean): void,

      /**
       * Updates the font size.
       **/
      updateFontSize(): void,

      /**
       * [Triggers a resize of the editor.]{: #VirtualRenderer.onResize}
       * @param force If `true`, recomputes the size, even if the height and width haven't changed
       * @param gutterWidth The width of the gutter in pixels
       * @param width The width of the editor in pixels
       * @param height The hiehgt of the editor, in pixels
       **/
      onResize(
          force: boolean,
          gutterWidth: number,
          width: number,
          height: number
      ): void,

      /**
       * Adjusts the wrap limit, which is the number of characters that can fit within the width of the edit area on screen.
       **/
      adjustWrapLimit(): void,

      /**
       * Identifies whether you want to have an animated scroll or not.
       * @param shouldAnimate Set to `true` to show animated scrolls
       **/
      setAnimatedScroll(shouldAnimate: boolean): void,

      /**
       * Returns whether an animated scroll happens or not.
       **/
      getAnimatedScroll(): boolean,

      /**
       * Identifies whether you want to show invisible characters or not.
       * @param showInvisibles Set to `true` to show invisibles
       **/
      setShowInvisibles(showInvisibles: boolean): void,

      /**
       * Returns whether invisible characters are being shown or not.
       **/
      getShowInvisibles(): boolean,

      /**
       * Identifies whether you want to show the print margin or not.
       * @param showPrintMargin Set to `true` to show the print margin
       **/
      setShowPrintMargin(showPrintMargin: boolean): void,

      /**
       * Returns whether the print margin is being shown or not.
       **/
      getShowPrintMargin(): boolean,

      /**
       * Identifies whether you want to show the print margin column or not.
       * @param showPrintMargin Set to `true` to show the print margin column
       **/
      setPrintMarginColumn(showPrintMargin: boolean): void,

      /**
       * Returns whether the print margin column is being shown or not.
       **/
      getPrintMarginColumn(): boolean,

      /**
       * Returns `true` if the gutter is being shown.
       **/
      getShowGutter(): boolean,

      /**
       * Identifies whether you want to show the gutter or not.
       * @param show Set to `true` to show the gutter
       **/
      setShowGutter(show: boolean): void,

      /**
       * Returns the root element containing this renderer.
       **/
      getContainerElement(): HTMLElement,

      /**
       * Returns the element that the mouse events are attached to
       **/
      getMouseEventTarget(): HTMLElement,

      /**
       * Returns the element to which the hidden text area is added.
       **/
      getTextAreaContainer(): HTMLElement,

      /**
       * [Returns the index of the first visible row.]{: #VirtualRenderer.getFirstVisibleRow}
       **/
      getFirstVisibleRow(): number,

      /**
       * Returns the index of the first fully visible row. "Fully" here means that the characters in the row are not truncated, that the top and the bottom of the row are on the screen.
       **/
      getFirstFullyVisibleRow(): number,

      /**
       * Returns the index of the last fully visible row. "Fully" here means that the characters in the row are not truncated, that the top and the bottom of the row are on the screen.
       **/
      getLastFullyVisibleRow(): number,

      /**
       * [Returns the index of the last visible row.]{: #VirtualRenderer.getLastVisibleRow}
       **/
      getLastVisibleRow(): number,

      /**
       * Sets the padding for all the layers.
       * @param padding A new padding value (in pixels)
       **/
      setPadding(padding: number): void,

      /**
       * Returns whether the horizontal scrollbar is set to be always visible.
       **/
      getHScrollBarAlwaysVisible(): boolean,

      /**
       * Identifies whether you want to show the horizontal scrollbar or not.
       * @param alwaysVisible Set to `true` to make the horizontal scroll bar visible
       **/
      setHScrollBarAlwaysVisible(alwaysVisible: boolean): void,

      /**
       * Schedules an update to all the front markers in the document.
       **/
      updateFrontMarkers(): void,

      /**
       * Schedules an update to all the back markers in the document.
       **/
      updateBackMarkers(): void,

      /**
       * Deprecated, (moved to [[EditSession]])
       **/
      addGutterDecoration(): void,

      /**
       * Deprecated, (moved to [[EditSession]])
       **/
      removeGutterDecoration(): void,

      /**
       * Redraw breakpoints.
       **/
      updateBreakpoints(): void,

      /**
       * Sets annotations for the gutter.
       * @param annotations An array containing annotations
       **/
      setAnnotations(annotations: any[]): void,

      /**
       * Updates the cursor icon.
       **/
      updateCursor(): void,

      /**
       * Hides the cursor icon.
       **/
      hideCursor(): void,

      /**
       * Shows the cursor icon.
       **/
      showCursor(): void,

      /**
       * Scrolls the cursor into the first visibile area of the editor
       **/
      scrollCursorIntoView(): void,

      /**
       * {:EditSession.getScrollTop}
       **/
      getScrollTop(): number,

      /**
       * {:EditSession.getScrollLeft}
       **/
      getScrollLeft(): number,

      /**
       * Returns the first visible row, regardless of whether it's fully visible or not.
       **/
      getScrollTopRow(): number,

      /**
       * Returns the last visible row, regardless of whether it's fully visible or not.
       **/
      getScrollBottomRow(): number,

      /**
       * Gracefully scrolls from the top of the editor to the row indicated.
       * @param row A row id
       **/
      scrollToRow(row: number): void,

      /**
       * Gracefully scrolls the editor to the row indicated.
       * @param line A line number
       * @param center If `true`, centers the editor the to indicated line
       * @param animate If `true` animates scrolling
       * @param callback Function to be called after the animation has finished
       **/
      scrollToLine(
          line: number,
          center: boolean,
          animate: boolean,
          callback: Function
      ): void,

      /**
       * Scrolls the editor to the y pixel indicated.
       * @param scrollTop The position to scroll to
       **/
      scrollToY(scrollTop: number): number,

      /**
       * Scrolls the editor across the x-axis to the pixel indicated.
       * @param scrollLeft The position to scroll to
       **/
      scrollToX(scrollLeft: number): number,

      /**
       * Scrolls the editor across both x- and y-axes.
       * @param deltaX The x value to scroll by
       * @param deltaY The y value to scroll by
       **/
      scrollBy(deltaX: number, deltaY: number): void,

      /**
       * Returns `true` if you can still scroll by either parameter, in other words, you haven't reached the end of the file or line.
       * @param deltaX The x value to scroll by
       * @param deltaY The y value to scroll by
       **/
      isScrollableBy(deltaX: number, deltaY: number): boolean,

      /**
       * Returns an object containing the `pageX` and `pageY` coordinates of the document position.
       * @param row The document row position
       * @param column The document column position
       **/
      textToScreenCoordinates(row: number, column: number): any,

      /**
       * Focuses the current container.
       **/
      visualizeFocus(): void,

      /**
       * Blurs the current container.
       **/
      visualizeBlur(): void,

      /**
       * undefined
       * @param position
       **/
      showComposition(position: number): void,

      /**
       * Sets the inner text of the current composition to `text`.
       * @param text A string of text to use
       **/
      setCompositionText(text: string): void,

      /**
       * Hides the current composition.
       **/
      hideComposition(): void,

      /**
       * [Sets a new theme for the editor. `theme` should exist, and be a directory path, like `ace/theme/textmate`.]{: #VirtualRenderer.setTheme}
       * @param theme The path to a theme
       **/
      setTheme(theme: string): void,

      /**
       * [Returns the path of the current theme.]{: #VirtualRenderer.getTheme}
       **/
      getTheme(): string,

      /**
       * [Adds a new class, `style`, to the editor.]{: #VirtualRenderer.setStyle}
       * @param style A class name
       **/
      setStyle(style: string): void,

      /**
       * [Removes the class `style` from the editor.]{: #VirtualRenderer.unsetStyle}
       * @param style A class name
       **/
      unsetStyle(style: string): void,

      /**
       * Destroys the text and cursor layers for this renderer.
       **/
      destroy(): void,
  };

  declare export type AceSelection = Selection & {
      isEmpty(): boolean,
  };

  declare export type Position = {|
      row: number,

      column: number,
  |};

  declare export type EditorChangeEvent = {
      start: Position,
      end: Position,
      action: string, // insert, remove
      lines: any[],
  };

  declare export type CommandManager = {
      byName: any,

      commands: any,

      platform: string,

      addCommands(commands: EditorCommand[]): void,

      removeCommands(commands: Array<string>): void,

      addCommand(command: EditorCommand): void,

      exec(name: string, editor: Editor, args: any): void,
  };

  declare export type Editor = {
      on(ev: string, callback: (e: any) => any): void,

      addEventListener(
          ev: "change",
          callback: (ev: EditorChangeEvent) => any
      ): void,
      addEventListener(ev: string, callback: Function): void,

      off(ev: string, callback: Function): void,

      removeListener(ev: string, callback: Function): void,

      removeEventListener(ev: string, callback: Function): void,

      inMultiSelectMode: boolean,

      selectMoreLines(n: number): void,

      onTextInput(text: string): void,

      onCommandKey(e: any, hashId: any, keyCode: any): void,

      commands: CommandManager,

      session: IEditSession,

      selection: AceSelection,

      renderer: VirtualRenderer,

      keyBinding: KeyBinding,

      container: HTMLElement,

      onSelectionChange(e: any): void,

      onChangeMode(e?: any): void,

      execCommand(command: string, args?: any): void,

      /**
       * Sets a Configuration Option
       **/
      setOption(optionName: any, optionValue: any): void,

      /**
       * Sets Configuration Options
       **/
      setOptions(keyValueTuples: any): void,

      /**
       * Get a Configuration Option
       **/
      getOption(name: any): any,

      /**
       * Get Configuration Options
       **/
      getOptions(): any,

      /**
       * Get rid of console warning by setting this to Infinity
       **/
      $blockScrolling: number,

      /**
       * Sets a new key handler, such as "vim" or "windows".
       * @param keyboardHandler The new key handler
       **/
      setKeyboardHandler(keyboardHandler: string): void,

      /**
       * Returns the keyboard handler, such as "vim" or "windows".
       **/
      getKeyboardHandler(): string,

      /**
       * Sets a new editsession to use. This method also emits the `'changeSession'` event.
       * @param session The new session to use
       **/
      setSession(session: IEditSession): void,

      /**
       * Returns the current session being used.
       **/
      getSession(): IEditSession,
      session: IEditSession,

      /**
       * Sets the current document to `val`.
       * @param val The new value to set for the document
       * @param cursorPos Where to set the new value. `undefined` or 0 is selectAll, -1 is at the document start, and 1 is at the end
       **/
      setValue(val: string, cursorPos?: number): string,

      /**
       * Returns the current session's content.
       **/
      getValue(): string,

      /**
       * Returns the currently highlighted selection.
       **/
      getSelection(): AceSelection,

      /**
       * {:VirtualRenderer.onResize}
       * @param force If `true`, recomputes the size, even if the height and width haven't changed
       **/
      resize(force?: boolean): void,

      /**
       * {:VirtualRenderer.setTheme}
       * @param theme The path to a theme
       **/
      setTheme(theme: string): void,

      /**
       * {:VirtualRenderer.getTheme}
       **/
      getTheme(): string,

      /**
       * {:VirtualRenderer.setStyle}
       * @param style A class name
       **/
      setStyle(style: string): void,

      /**
       * {:VirtualRenderer.unsetStyle}
       **/
      unsetStyle(): void,

      /**
       * Set a new font size (in pixels) for the editor text.
       * @param size A font size ( _e.g._ "12px")
       **/
      setFontSize(size: string): void,

      /**
       * Brings the current `textInput` into focus.
       **/
      focus(): void,

      /**
       * Returns `true` if the current `textInput` is in focus.
       **/
      isFocused(): void,

      /**
       * Blurs the current `textInput`.
       **/
      blur(): void,

      /**
       * Emitted once the editor comes into focus.
       **/
      onFocus(): void,

      /**
       * Emitted once the editor has been blurred.
       **/
      onBlur(): void,

      /**
       * Emitted whenever the document is changed.
       * @param e Contains a single property, `data`, which has the delta of changes
       **/
      onDocumentChange(e: any): void,

      /**
       * Emitted when the selection changes.
       **/
      onCursorChange(): void,

      /**
       * Returns the string of text currently highlighted.
       **/
      getCopyText(): string,

      /**
       * Called whenever a text "copy" happens.
       **/
      onCopy(): void,

      /**
       * Called whenever a text "cut" happens.
       **/
      onCut(): void,

      /**
       * Called whenever a text "paste" happens.
       * @param text The pasted text
       **/
      onPaste(text: string): void,

      /**
       * Inserts `text` into wherever the cursor is pointing.
       * @param text The new text to add
       **/
      insert(text: string): void,

      /**
       * Pass in `true` to enable overwrites in your session, or `false` to disable. If overwrites is enabled, any text you enter will type over any text after it. If the value of `overwrite` changes, this function also emites the `changeOverwrite` event.
       * @param overwrite Defines wheter or not to set overwrites
       **/
      setOverwrite(overwrite: boolean): void,

      /**
       * Returns `true` if overwrites are enabled, `false` otherwise.
       **/
      getOverwrite(): boolean,

      /**
       * Sets the value of overwrite to the opposite of whatever it currently is.
       **/
      toggleOverwrite(): void,

      /**
       * Sets how fast the mouse scrolling should do.
       * @param speed A value indicating the new speed (in milliseconds)
       **/
      setScrollSpeed(speed: number): void,

      /**
       * Returns the value indicating how fast the mouse scroll speed is (in milliseconds).
       **/
      getScrollSpeed(): number,

      /**
       * Sets the delay (in milliseconds) of the mouse drag.
       * @param dragDelay A value indicating the new delay
       **/
      setDragDelay(dragDelay: number): void,

      /**
       * Returns the current mouse drag delay.
       **/
      getDragDelay(): number,

      /**
       * Indicates how selections should occur.
       * By default, selections are set to "line". There are no other styles at the moment,
       * although this code change in the future.
       * This function also emits the `'changeSelectionStyle'` event.
       * @param style The new selection style
       **/
      setSelectionStyle(style: string): void,

      /**
       * Returns the current selection style.
       **/
      getSelectionStyle(): string,

      /**
       * Determines whether or not the current line should be highlighted.
       * @param shouldHighlight Set to `true` to highlight the current line
       **/
      setHighlightActiveLine(shouldHighlight: boolean): void,

      /**
       * Returns `true` if current lines are always highlighted.
       **/
      getHighlightActiveLine(): void,

      /**
       * Determines if the currently selected word should be highlighted.
       * @param shouldHighlight Set to `true` to highlight the currently selected word
       **/
      setHighlightSelectedWord(shouldHighlight: boolean): void,

      /**
       * Returns `true` if currently highlighted words are to be highlighted.
       **/
      getHighlightSelectedWord(): boolean,

      /**
       * If `showInvisibiles` is set to `true`, invisible characters&mdash,like spaces or new lines&mdash,are show in the editor.
       * @param showInvisibles Specifies whether or not to show invisible characters
       **/
      setShowInvisibles(showInvisibles: boolean): void,

      /**
       * Returns `true` if invisible characters are being shown.
       **/
      getShowInvisibles(): boolean,

      /**
       * If `showPrintMargin` is set to `true`, the print margin is shown in the editor.
       * @param showPrintMargin Specifies whether or not to show the print margin
       **/
      setShowPrintMargin(showPrintMargin: boolean): void,

      /**
       * Returns `true` if the print margin is being shown.
       **/
      getShowPrintMargin(): boolean,

      /**
       * Sets the column defining where the print margin should be.
       * @param showPrintMargin Specifies the new print margin
       **/
      setPrintMarginColumn(showPrintMargin: number): void,

      /**
       * Returns the column number of where the print margin is.
       **/
      getPrintMarginColumn(): number,

      /**
       * If `readOnly` is true, then the editor is set to read-only mode, and none of the content can change.
       * @param readOnly Specifies whether the editor can be modified or not
       **/
      setReadOnly(readOnly: boolean): void,

      /**
       * Returns `true` if the editor is set to read-only mode.
       **/
      getReadOnly(): boolean,

      /**
       * Specifies whether to use behaviors or not. ["Behaviors" in this case is the auto-pairing of special characters, like quotation marks, parenthesis, or brackets.]{: #BehaviorsDef}
       * @param enabled Enables or disables behaviors
       **/
      setBehavioursEnabled(enabled: boolean): void,

      /**
       * Returns `true` if the behaviors are currently enabled. {:BehaviorsDef}
       **/
      getBehavioursEnabled(): boolean,

      /**
       * Specifies whether to use wrapping behaviors or not, i.e. automatically wrapping the selection with characters such as brackets
       * when such a character is typed in.
       * @param enabled Enables or disables wrapping behaviors
       **/
      setWrapBehavioursEnabled(enabled: boolean): void,

      /**
       * Returns `true` if the wrapping behaviors are currently enabled.
       **/
      getWrapBehavioursEnabled(): void,

      /**
       * Indicates whether the fold widgets are shown or not.
       * @param show Specifies whether the fold widgets are shown
       **/
      setShowFoldWidgets(show: boolean): void,

      /**
       * Returns `true` if the fold widgets are shown.
       **/
      getShowFoldWidgets(): void,

      /**
       * Removes words of text from the editor. A "word" is defined as a string of characters bookended by whitespace.
       * @param dir The direction of the deletion to occur, either "left" or "right"
       **/
      remove(dir: string): void,

      /**
       * Removes the word directly to the right of the current selection.
       **/
      removeWordRight(): void,

      /**
       * Removes the word directly to the left of the current selection.
       **/
      removeWordLeft(): void,

      /**
       * Removes all the words to the left of the current selection, until the start of the line.
       **/
      removeToLineStart(): void,

      /**
       * Removes all the words to the right of the current selection, until the end of the line.
       **/
      removeToLineEnd(): void,

      /**
       * Splits the line at the current selection (by inserting an `'\n'`).
       **/
      splitLine(): void,

      /**
       * Transposes current line.
       **/
      transposeLetters(): void,

      /**
       * Converts the current selection entirely into lowercase.
       **/
      toLowerCase(): void,

      /**
       * Converts the current selection entirely into uppercase.
       **/
      toUpperCase(): void,

      /**
       * Inserts an indentation into the current cursor position or indents the selected lines.
       **/
      indent(): void,

      /**
       * Indents the current line.
       **/
      blockIndent(): void,

      /**
       * Outdents the current line.
       **/
      blockOutdent(arg?: string): void,

      /**
       * Given the currently selected range, this function either comments all the lines, or uncomments all of them.
       **/
      toggleCommentLines(): void,

      /**
       * Works like [[EditSession.getTokenAt]], except it returns a number.
       **/
      getNumberAt(): number,

      /**
       * If the character before the cursor is a number, this functions changes its value by `amount`.
       * @param amount The value to change the numeral by (can be negative to decrease value)
       **/
      modifyNumber(amount: number): void,

      /**
       * Removes all the lines in the current selection
       **/
      removeLines(): void,

      /**
       * Shifts all the selected lines down one row.
       **/
      moveLinesDown(): number,

      /**
       * Shifts all the selected lines up one row.
       **/
      moveLinesUp(): number,

      /**
       * Moves a range of text from the given range to the given position. `toPosition` is an object that looks like this:
       * ```json
       * { row: newRowLocation, column: newColumnLocation }
       * ```
       * @param fromRange The range of text you want moved within the document
       * @param toPosition The location (row and column) where you want to move the text to
       **/
      moveText(fromRange: Range, toPosition: any): Range,

      /**
       * Copies all the selected lines up one row.
       **/
      copyLinesUp(): number,

      /**
       * Copies all the selected lines down one row.
       **/
      copyLinesDown(): number,

      /**
       * {:VirtualRenderer.getFirstVisibleRow}
       **/
      getFirstVisibleRow(): number,

      /**
       * {:VirtualRenderer.getLastVisibleRow}
       **/
      getLastVisibleRow(): number,

      /**
       * Indicates if the row is currently visible on the screen.
       * @param row The row to check
       **/
      isRowVisible(row: number): boolean,

      /**
       * Indicates if the entire row is currently visible on the screen.
       * @param row The row to check
       **/
      isRowFullyVisible(row: number): boolean,

      /**
       * Selects the text from the current position of the document until where a "page down" finishes.
       **/
      selectPageDown(): void,

      /**
       * Selects the text from the current position of the document until where a "page up" finishes.
       **/
      selectPageUp(): void,

      /**
       * Shifts the document to wherever "page down" is, as well as moving the cursor position.
       **/
      gotoPageDown(): void,

      /**
       * Shifts the document to wherever "page up" is, as well as moving the cursor position.
       **/
      gotoPageUp(): void,

      /**
       * Scrolls the document to wherever "page down" is, without changing the cursor position.
       **/
      scrollPageDown(): void,

      /**
       * Scrolls the document to wherever "page up" is, without changing the cursor position.
       **/
      scrollPageUp(): void,

      /**
       * Moves the editor to the specified row.
       **/
      scrollToRow(): void,

      /**
       * Scrolls to a line. If `center` is `true`, it puts the line in middle of screen (or attempts to).
       * @param line The line to scroll to
       * @param center If `true`
       * @param animate If `true` animates scrolling
       * @param callback Function to be called when the animation has finished
       **/
      scrollToLine(
          line: number,
          center: boolean,
          animate: boolean,
          callback: Function
      ): void,

      /**
       * Attempts to center the current selection on the screen.
       **/
      centerSelection(): void,

      /**
       * Gets the current position of the cursor.
       **/
      getCursorPosition(): Position,

      /**
       * Returns the screen position of the cursor.
       **/
      getCursorPositionScreen(): number,

      /**
       * {:Selection.getRange}
       **/
      getSelectionRange(): Range,

      /**
       * Selects all the text in editor.
       **/
      selectAll(): void,

      /**
       * {:Selection.clearSelection}
       **/
      clearSelection(): void,

      /**
       * Moves the cursor to the specified row and column. Note that this does not de-select the current selection.
       * @param row The new row number
       * @param column The new column number
       **/
      moveCursorTo(row: number, column?: number, animate?: boolean): void,

      /**
       * Moves the cursor to the position indicated by `pos.row` and `pos.column`.
       * @param position An object with two properties, row and column
       **/
      moveCursorToPosition(position: Position): void,

      /**
       * Moves the cursor's row and column to the next matching bracket.
       **/
      jumpToMatching(): void,

      /**
       * Moves the cursor to the specified line number, and also into the indiciated column.
       * @param lineNumber The line number to go to
       * @param column A column number to go to
       * @param animate If `true` animates scolling
       **/
      gotoLine(lineNumber: number, column?: number, animate?: boolean): void,

      /**
       * Moves the cursor to the specified row and column. Note that this does de-select the current selection.
       * @param row The new row number
       * @param column The new column number
       **/
      navigateTo(row: number, column: number): void,

      /**
       * Moves the cursor up in the document the specified number of times. Note that this does de-select the current selection.
       * @param times The number of times to change navigation
       **/
      navigateUp(times?: number): void,

      /**
       * Moves the cursor down in the document the specified number of times. Note that this does de-select the current selection.
       * @param times The number of times to change navigation
       **/
      navigateDown(times?: number): void,

      /**
       * Moves the cursor left in the document the specified number of times. Note that this does de-select the current selection.
       * @param times The number of times to change navigation
       **/
      navigateLeft(times?: number): void,

      /**
       * Moves the cursor right in the document the specified number of times. Note that this does de-select the current selection.
       * @param times The number of times to change navigation
       **/
      navigateRight(times: number): void,

      /**
       * Moves the cursor to the start of the current line. Note that this does de-select the current selection.
       **/
      navigateLineStart(): void,

      /**
       * Moves the cursor to the end of the current line. Note that this does de-select the current selection.
       **/
      navigateLineEnd(): void,

      /**
       * Moves the cursor to the end of the current file. Note that this does de-select the current selection.
       **/
      navigateFileEnd(): void,

      /**
       * Moves the cursor to the start of the current file. Note that this does de-select the current selection.
       **/
      navigateFileStart(): void,

      /**
       * Moves the cursor to the word immediately to the right of the current position. Note that this does de-select the current selection.
       **/
      navigateWordRight(): void,

      /**
       * Moves the cursor to the word immediately to the left of the current position. Note that this does de-select the current selection.
       **/
      navigateWordLeft(): void,

      /**
       * Replaces the first occurance of `options.needle` with the value in `replacement`.
       * @param replacement The text to replace with
       * @param options The [[Search `Search`]] options to use
       **/
      replace(replacement: string, options?: any): void,

      /**
       * Replaces all occurances of `options.needle` with the value in `replacement`.
       * @param replacement The text to replace with
       * @param options The [[Search `Search`]] options to use
       **/
      replaceAll(replacement: string, options?: any): void,

      /**
       * {:Search.getOptions} For more information on `options`, see [[Search `Search`]].
       **/
      getLastSearchOptions(): any,

      /**
       * Attempts to find `needle` within the document. For more information on `options`, see [[Search `Search`]].
       * @param needle The text to search for (optional)
       * @param options An object defining various search properties
       * @param animate If `true` animate scrolling
       **/
      find(needle: string, options?: any, animate?: boolean): void,

      /**
       * Performs another search for `needle` in the document. For more information on `options`, see [[Search `Search`]].
       * @param options search options
       * @param animate If `true` animate scrolling
       **/
      findNext(options?: any, animate?: boolean): void,

      /**
       * Performs a search for `needle` backwards. For more information on `options`, see [[Search `Search`]].
       * @param options search options
       * @param animate If `true` animate scrolling
       **/
      findPrevious(options?: any, animate?: boolean): void,

      /**
       * {:UndoManager.undo}
       **/
      undo(): void,

      /**
       * {:UndoManager.redo}
       **/
      redo(): void,

      /**
       * Cleans up the entire editor.
       **/
      destroy(): void,
  };

  declare export default class AceEditor extends React$Component<Props> {
      editor: Editor;
  }

  declare function acequire(moduleName: string): any;
}
