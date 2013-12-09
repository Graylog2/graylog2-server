( function( $ ) {
    var SampleMessageLoader = function(element, options) {
        var element = $( element );
        element.addClass( "sampleMessageLoader" );

        subcontainer = options['subcontainer'];
        selector = options['selector'];
        spinner = options['spinner'];
        messageContainer = options['message'];
        callback = options['callback'];

        $('button', subcontainer).on("click", function(e) {
            subcontainer.hide();
            selector.show();
        });

        $('form', selector).on("submit", function(e) {
            e.preventDefault();
            var index = $("input[name=index]", $(this)).val();
            var messageId = $("input[name=message_id]", $(this)).val();

            loadMessage(index, messageId);
        });

        function loadMessage(index, messageId, url) {
            selector.hide();
            showSpinner(spinner);

            if (url == undefined) {
                url = '/a/messages/' + index + '/' + messageId;
            }

            $.ajax({
                url: url,
                success: function(data) {
                    showMessage(messageContainer, data.fields, data.id);
                    messageContainer.trigger("sampleMessageChanged", data);
                    hideSpinner(spinner);
                    selector.hide();
                    messageContainer.show();
                    if (callback != undefined) {
                        callback(data);
                    }
                },
                error: function() {
                    showError("Could not load message. Make sure that ID and index are correct.");
                    selector.show();
                },
                complete: function() {
                    hideSpinner(spinner);
                }
            });
        }

        function showSpinner(element) {
            element.show();
        }

        function hideSpinner(element) {
            element.hide();
        }

        function showMessage(container, msg, messageid) {
            var list = $("dl", container);
            var placeHolders = $("[data-occurrence=repeat]", list);

            for (var field in msg) {
                var newElems = placeHolders.clone();
                var i = 0;
                newElems.each( function(c, elem) {
                    var newElem = elem.outerHTML.replace(/\{\{field\}\}/g, field).replace(/\{\{value\}\}/g, msg[field]);
                    var newElem = $( newElem ).removeAttr("data-occurrence");
                    newElem.appendTo(list);
                });
            }

            placeHolders.remove();
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
