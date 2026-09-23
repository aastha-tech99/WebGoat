$(document).ready( () => {
    var editor = ace.edit("editor");
    editor.setTheme("ace/theme/monokai");
    editor.session.setMode("ace/mode/java");

    editor.getSession().on("change", () => {
        var currentValue = ace_collect();
        setTimeout( () => {
            $("#codesubmit input[name='editor']").val(currentValue);
        }, 20);
    });


});

function ace_collect() {
    var editor = ace.edit("editor");
    var code = editor.getValue();
    return code;
}
