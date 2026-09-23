$(document).ready(function() {
    var fadeOutScheduled = false;
    if (!fadeOutScheduled) {
        fadeOutScheduled = true;
        window.setTimeout(function () {
            $(".fileUploadAlert").fadeTo(500, 0).slideUp(500, function () {
                $(this).hide();
            });
        }, 4000);
    }
});

$(document).on('click','.fa-files-o',function(){
    document.execCommand('copy');
});
