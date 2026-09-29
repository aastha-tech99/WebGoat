webgoat.customjs.feedback = function() {
    var data = {};
    var allowedFields = ['name', 'email', 'subject', 'message', 'comment', 'text', 'feedback', 'rating', 'title'];
    $('#csrf-feedback').find('input, textarea, select').each(function(i, field) {
        if (field.name !== '__proto__' && field.name !== 'constructor' && field.name !== 'prototype') {
            Object.defineProperty(data, field.name, {
                value: field.value,
                writable: true,
                enumerable: true,
                configurable: true
            });
        }
    });
    return JSON.stringify(data);
}
