var dataFetched = false;

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#x27;');
}

function selectUser() {

    var newEmployeeID = $("#UserSelect").val();
    var source = document.getElementById(newEmployeeID);
    var target = document.getElementById("employeeRecord");
    while (target.firstChild) {
        target.removeChild(target.firstChild);
    }
    if (source) {
        Array.from(source.childNodes).forEach(function(child) {
            target.appendChild(child.cloneNode(true));
        });
    }
}

function fetchUserData() {
    if (!dataFetched) {
        dataFetched = true;
        ajaxFunction(document.getElementById("userID").value);
    }
}

function ajaxFunction(userId) {
    $.get("clientSideFiltering/salaries?userId=" + userId, function (result, status) {
        var html = "<table border = '1' width = '90%' align = 'center'";
        html = html + '<tr>';
        html = html + '<td>UserID</td>';
        html = html + '<td>First Name</td>';
        html = html + '<td>Last Name</td>';
        html = html + '<td>SSN</td>';
        html = html + '<td>Salary</td>';

        for (var i = 0; i < result.length; i++) {
            html = html + '<tr id="' + escapeHtml(result[i].UserID) + '">';
            html = html + '<td>' + escapeHtml(result[i].UserID) + '</td>';
            html = html + '<td>' + escapeHtml(result[i].FirstName) + '</td>';
            html = html + '<td>' + escapeHtml(result[i].LastName) + '</td>';
            html = html + '<td>' + escapeHtml(result[i].SSN) + '</td>';
            html = html + '<td>' + escapeHtml(result[i].Salary) + '</td>';
            html = html + '</tr>';
        }
        html = html + '</tr></table>';

        var newdiv = document.createElement("div");
        newdiv.innerHTML = html;
        var container = document.getElementById("hiddenEmployeeRecords");
        container.appendChild(newdiv);
    });
}
