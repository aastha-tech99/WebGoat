var dataFetched = false;

function selectUser() {

    var newEmployeeID = $("#UserSelect").val();
    var sourceEl = document.getElementById(newEmployeeID);
    var targetEl = document.getElementById("employeeRecord");
    while (targetEl.firstChild) {
        targetEl.removeChild(targetEl.firstChild);
    }
    if (sourceEl) {
        Array.from(sourceEl.childNodes).forEach(function (child) {
            targetEl.appendChild(child.cloneNode(true));
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
        var table = document.createElement("table");
        table.setAttribute("border", "1");
        table.setAttribute("width", "90%");
        table.setAttribute("align", "center");

        var headerRow = document.createElement("tr");
        ["UserID", "First Name", "Last Name", "SSN", "Salary"].forEach(function (text) {
            var td = document.createElement("td");
            td.textContent = text;
            headerRow.appendChild(td);
        });
        table.appendChild(headerRow);

        for (var i = 0; i < result.length; i++) {
            var row = document.createElement("tr");
            row.id = result[i].UserID;
            [result[i].UserID, result[i].FirstName, result[i].LastName, result[i].SSN, result[i].Salary].forEach(function (value) {
                var td = document.createElement("td");
                td.textContent = value;
                row.appendChild(td);
            });
            table.appendChild(row);
        }

        var newdiv = document.createElement("div");
        newdiv.appendChild(table);
        var container = document.getElementById("hiddenEmployeeRecords");
        container.appendChild(newdiv);
    });
}
