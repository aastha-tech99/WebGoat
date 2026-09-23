// In-memory token store to avoid localStorage which is vulnerable to XSS
var tokenStore = {};

$(document).ready(function () {
    // Fetch password from server configuration instead of hardcoding in source
    $.ajax({
        type: 'GET',
        url: 'JWT/refresh/appConfig'
    }).success(function (config) {
        login('Jerry', config.defaultPassword);
    });
})

function login(user, pw) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({user: user, password: pw})
    }).success(
        function (response) {
            tokenStore.access_token = response['access_token'];
            tokenStore.refresh_token = response['refresh_token'];
        }
    )
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + (tokenStore.access_token || '');
    return headers_to_set;
}

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + (tokenStore.access_token || '')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({refresh_token: tokenStore.refresh_token})
    }).success(
        function () {
            tokenStore.access_token = apiToken;
            tokenStore.refresh_token = refreshToken;
        }
    )
}
