function submit_secret_credentials() {
    var xhttp = new XMLHttpRequest();
    xhttp['open']('POST', 'InsecureLogin/login', true);
    xhttp.send(JSON.stringify({username: "CaptainJack", password: "BlackPearl"}));
}
