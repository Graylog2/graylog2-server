( function( $ ) {
    var SampleMessageLoader = function(element, options) {
        var element = $( element );
        element.addClass( "sampleMessageLoader" );

        subcontainer = options['subcontainer'];
        selector = options['selector'];
        spinner = options['spinner'];
        messageContainer = options['message'];
        callback = options['callback'];

        selectorButton = options['selectorButton'] || $('button', subcontainer);
        recentButton = options['recentButton'];

        selectorButton.on("click", function(e) {
            subcontainer.hide();
            selector.show();
        });

        if (recentButton) {
            recentButton.on("click", function(e) {
                var nodeId = $(this).attr("data-node-id");
                var inputId = $(this).attr("data-input-id");
                var url = '/a/system/inputs/' + nodeId + '/' + inputId + '/recent_message'; // url prefix applied when doing the ajax call!

                loadMessage(undefined, undefined, url);
            });
        }

        $('form', selector).on("submit", function(e) {
            e.preventDefault();
            var index = $("input[name=index]", $(this)).val().trim();
            var messageId = $("input[name=message_id]", $(this)).val().trim();

            loadMessage(index, messageId);
        });

        function loadMessage(index, messageId, url) {
            selector.hide();
            showSpinner(spinner);

            if (url == undefined) {
                url = '/a/messages/' + index + '/' + messageId;
            }

            $.ajax({
                url: appPrefixed(url),
                success: function(data) {
                    showMessage(messageContainer, data.formatted_fields, data.id, data.index);
                    selector.hide();
                    messageContainer.show();

                    $.ajax({
                        url: appPrefixed(url),
                        success: function(data) {
                            jQuery.data(document.body, "message", data);
                            messageContainer.trigger("sampleMessageChanged", data);
                            if (callback != undefined) {
                                callback(data);
                            }
                            hideSpinner(spinner);
                        },
                        error: function() {
                            showError("Could not load message. Make sure that ID and index are correct.");
                            selector.show();
                            return;
                        }
                    });
                },
                error: function() {
                    showError("Could not load message. Make sure that ID and index are correct.");
                    selector.show();
                    return;
                },
                complete: function() {
                    hideSpinner(spinner);
                }
            })
        }

        function showSpinner(element) {
            element.show();
        }

        function hideSpinner(element) {
            element.hide();
        }

        function showMessage(container, msg, messageid, messageIndex) {
            var oldContainer = jQuery.data(container, "oldMessageContainer");
            if (oldContainer != undefined)
                container.html(oldContainer);

            jQuery.data(container, "oldMessageContainer", container.html());
            var list = $("dl", container);
            var placeHolders = $("[data-occurrence=repeat]", list);

            for (var field in msg) {
                var newElems = placeHolders.clone();
                var i = 0;
                newElems.each( function(c, elem) {
                    var newElem = elem.outerHTML.replace(/\{\{field\}\}/g, field)
                        .replace(/\{\{value\}\}/g, htmlEscape(msg[field]))
                        .replace(/\{\{raw-value\}\}/g, htmlEscape(msg[field]).replace(/"/g, "&quot;"));
                    var newElem = $( newElem ).removeAttr("data-occurrence");
                    newElem.appendTo(list);
                });
            }

            placeHolders.remove();
            container.attr("data-id", messageid);
            container.attr("data-index", messageIndex);
            container.html(container.html().replace(/\{\{messageId\}\}/g, messageid));
        }
    };

    $.fn.sampleMessageLoader = function( options ){
        return this.each( function() {
            var element = $( this );
            if (element.data('sampleMessageLoader')) return;
            var sampleMessageLoader = new SampleMessageLoader(this, options);
            element.data('sampleMessageLoader', sampleMessageLoader);
        });
    };
}) ( jQuery );
