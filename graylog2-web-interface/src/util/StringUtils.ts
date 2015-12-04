'use strict';

var StringUtils = {
    tempDocument: document.createElement('textarea'),
    capitalizeFirstLetter(text: string): string {
        return text.charAt(0).toUpperCase() + text.slice(1);
    },
    escapeHTML(text: string): string {
        this.tempDocument.textContent = text;
        return this.tempDocument.innerHTML;
    },
    unescapeHTML(text: string): string {
        this.tempDocument.innerHTML = text;
        return this.tempDocument.textContent;
    },
};

export = StringUtils;