$(document).ready(function () {
    //-- Click on detail
    $("ul.menu-items > li").on("click", function () {
        $("ul.menu-items > li").removeClass("active");
        $(this).addClass("active");
    })

    $(".attr,.attr2").on("click", function () {
        var clase = $(this).attr("class");

        $("." + clase).removeClass("active");
        $(this).addClass("active");
    })

    //-- Click on QUANTITY
    $(".btn-minus").on("click", function () {
        var now = $(".quantity").val();
        if ($.isNumeric(now)) {
            if (parseInt(now) - 1 > 0) {
                now--;
            }
            $(".quantity").val(now);
            $('#price').text(now * 899);
        } else {
            $(".quantity").val("1");
            $('#price').text(899);
        }
        calculate();
    })
    $(".btn-plus").on("click", function () {
        var now = $(".quantity").val();
        if ($.isNumeric(now)) {
            $(".quantity").val(parseInt(now) + 1);
        } else {
            $(".quantity").val("1");
        }
        calculate();
    })
    var couponUsageCount = {};
    var maxCouponUses = 1;
    $(".checkoutCode").on("blur", function () {
        var checkoutCode = $(".checkoutCode").val();
        if (couponUsageCount[checkoutCode] && couponUsageCount[checkoutCode] >= maxCouponUses) {
            $('#discount').text(0);
            calculate();
            return;
        }
        $.get("clientSideFiltering/challenge-store/coupons/" + checkoutCode, function (result, status) {
            var discount = result.discount;
            if (discount > 0) {
                couponUsageCount[checkoutCode] = (couponUsageCount[checkoutCode] || 0) + 1;
                $('#discount').text(discount);
                calculate();
            } else {
                $('#discount').text(0);
                calculate();
            }
        });
    })

    function calculate() {
        var d = $('#discount').text();
        var price = $('#price').val();
        var quantity = parseInt($(".quantity").val());
        if (d > 0) {
            $('#price').text((quantity * (899 - (899 * d / 100))).toFixed(2));

        } else {
            $('#price').text(quantity * 899);
        }
    }
})
