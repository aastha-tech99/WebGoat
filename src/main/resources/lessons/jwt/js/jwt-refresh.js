// In-memory token store to avoid exposing JWTs via localStorage (XSS-accessible).
var tokenStore = (function () {
    var tokens = {};
    return {
        setItem: function (key, value) { tokens[key] = value; },
        getItem: function (key) { return tokens[key] || null; }
    };
})();

$(document).ready(function () {
    login('Jerry');
})

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Placeholder credential for WebGoat JWT lesson demonstration, not a real secret
        data: JSON.stringify({user: user, password: "bm5nhSkxCXZkKRy4"})
    }).success(
        function (response) {
            tokenStore.setItem('access_token', response['access_token']);
            tokenStore.setItem('refresh_token', response['refresh_token']);
        }
    )
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + tokenStore.getItem('access_token');
    return headers_to_set;
}

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    tokenStore.getItem('refreshToken');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + tokenStore.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({refresh_token: tokenStore.getItem('refresh_token')})
    }).success(
        function () {
            tokenStore.setItem('access_token', apiToken);
            tokenStore.setItem('refresh_token', refreshToken);
        }
    )
}
