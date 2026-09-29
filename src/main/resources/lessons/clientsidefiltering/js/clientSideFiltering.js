var dataFetched = false;

function selectUser() {

    var newEmployeeID = $("#UserSelect").val();
    document.getElementById("employeeRecord").innerHTML = document.getElementById(newEmployeeID).innerHTML;
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

        result.forEach(function (item) {
            html = html + '<tr id = "' + item.UserID + '"</tr>';
            html = html + '<td>' + item.UserID + '</td>';
            html = html + '<td>' + item.FirstName + '</td>';
            html = html + '<td>' + item.LastName + '</td>';
            html = html + '<td>' + item.SSN + '</td>';
            html = html + '<td>' + item.Salary + '</td>';
            html = html + '</tr>';
        });
        html = html + '</tr></table>';

        var newdiv = document.createElement("div");
        newdiv.innerHTML = html;
        var container = document.getElementById("hiddenEmployeeRecords");
        container.appendChild(newdiv);
    });
}
