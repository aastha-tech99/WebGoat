$(document).ready(function () {
    $("#postComment").on("click", function () {
        var commentInput = $("#commentInput").val();
        if (!commentInput || commentInput.trim().length === 0) {
            return;
        }
        // Verify user session is authenticated before allowing comment submission
        $.ajax({
            type: 'GET',
            url: 'CrossSiteScriptingStored/stored-xss',
            dataType: 'json'
        }).then(function () {
            // User session verified, proceed with comment submission
            $.ajax({
                type: 'POST',
                url: 'CrossSiteScriptingStored/stored-xss',
                data: JSON.stringify({text: commentInput}),
                contentType: "application/json",
                dataType: 'json'
            }).then(
                function () {
                    getChallenges();
                    $("#commentInput").val('');
                }
            ).fail(function () {
                $("#commentInput").val('');
            });
        }).fail(function () {
            // User not verified or session expired
            $("#commentInput").val('');
        });
    })

    var html = '<li class="comment">' +
        '<div class="pull-left">' +
        '<img class="avatar" src="images/avatar1.png" alt="avatar"/>' +
        '</div>' +
        '<div class="comment-body">' +
        '<div class="comment-heading">' +
        '<h4 class="user">USER</h4>' +
        '<h5 class="time">DATETIME</h5>' +
        '</div>' +
        '<p>COMMENT</p>' +
        '</div>' +
        '</li>';

    getChallenges();

    function getChallenges() {
        $("#list").empty();
        $.get('CrossSiteScriptingStored/stored-xss', function (result, status) {
            for (var i = 0; i < result.length; i++) {
                var comment = html.replace('USER', result[i].user);
                comment = comment.replace('DATETIME', result[i].dateTime);
                comment = comment.replace('COMMENT', result[i].text);
                $("#list").append(comment);
            }

        });
    }
})
