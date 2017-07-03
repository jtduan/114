// 114挂号脚本js版
var hospitalId = "142";
var departmentId = "200039530";
var dockerName = "李志刚";
var date = "2017-07-06";
var dutyCode = "1";
timer1 = setInterval(function () {
    $.ajax({
        url: "/dpt/partduty.htm",
        type: "post",
        async: false,
        data: {
            "hospitalId": hospitalId,
            "departmentId": departmentId,
            "dutyCode": dutyCode,
            "dutyDate": date,
            "isAjax": "true"
        },
        success: function (json) {
            var dockers = JSON.parse(json).data;
            for (var i = dockers.length - 1; i >= 0; i--) {
                if (dockers[i].remainAvailableNumber > 0 && dockers[i].doctorName.indexOf(dockerName) >= 0) {
                    var a = document.createElement("a");
                    a.setAttribute("href", "/order/confirm/" + hospitalId + "-" + departmentId + "-" + dockers[i].doctorId + "-" + dockers[i].dutySourceId + ".htm");
                    a.style.display = "none";
                    a.setAttribute("class", "ksorder_dr1_syhy");
                    document.body.appendChild(a);
                    window.clearInterval(timer1);
                    setTimeout(function () {
                        a.click();
                    }, 800);
                    return;
                }
            }
            console.log(json);
        }
    });
}, 400);