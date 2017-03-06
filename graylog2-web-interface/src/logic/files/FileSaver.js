const FileSaver = {
  save(data, filename, mime, charset) {
    const link = document.createElement('a');

    const effectiveCharset = charset ? `;charset=${charset}` : '';
    const contentType = charset ? `${mime}${effectiveCharset}` : mime;

    // On modern browsers (Chrome and Firefox), use download property and a temporary link
    if (link.download !== undefined) {
      link.download = filename;
      link.href = `data:${contentType},${encodeURIComponent(data)}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      return;
    }

    // On IE >= 10, use msSaveOrOpenBlob
    if (window.navigator && typeof window.navigator.msSaveOrOpenBlob === 'function') {
      const blob = new Blob([data], { type: contentType });
      window.navigator.msSaveOrOpenBlob(blob, filename);

      return;
    }

    try {
      // On Safari and other browsers, try to open the JSON as attachment
      location.href = `data:application/attachment${effectiveCharset},${encodeURIComponent(data)}`;
    } catch (e) {
      // If nothing else works, open the JSON as plain text in the browser
      location.href = `data:text/plain${effectiveCharset},${encodeURIComponent(data)}`;
    }
  },
};

export default FileSaver;
