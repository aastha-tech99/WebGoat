webgoat.customjs.feedback = function() {
    var data = {};
    $('#csrf-feedback').find('input, textarea, select').each(function(i, field) {
        if (field.name !== '__proto__' && field.name !== 'constructor' && field.name !== 'prototype') {
            data[field.name] = field.value;
        }
    });
    return JSON.stringify(data);
}
