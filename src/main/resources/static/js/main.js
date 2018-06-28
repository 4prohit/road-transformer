var googleMap;
var markers = [];
var defaultConfidence = 100;

var greenRoadImage = "https://s3-us-west-2.amazonaws.com/hackdaysample/green_road.png";
var yellowRoadImage = "https://s3-us-west-2.amazonaws.com/hackdaysample/yellow_road.png";
var redRoadImage = "https://s3-us-west-2.amazonaws.com/hackdaysample/red_road.png";

function InitializeMap() {
    var latlng = new google.maps.LatLng(1.352083, 103.819836);
    var myOptions =
        {
            zoom: 12,
            center: latlng,
            mapTypeId: google.maps.MapTypeId.ROADMAP,
            disableDefaultUI: true
        };
    googleMap = new google.maps.Map(document.getElementById("googleMap"), myOptions);
}

function getStreetConfidence(confidence) {
    $.ajax({
        type: "GET",
        url: "/v1/streetConfidence?confidence=" + confidence,
        dataType: "json",
        beforeSend: function () {
            $('#spinner').show();
        },
        complete: function () {
            $('#spinner').hide();
        },
        success: function (data) {
            console.log("StreetConfidence Response -> ");
            console.log(data);
            if (data.length <= 0) {
                alert("No data available");
            } else {
                clearOverlays();
                $.each(data, function (i, streetConfidenceObj) {
                    var latlng = new google.maps.LatLng(streetConfidenceObj.streetDetail.latitude, streetConfidenceObj.streetDetail.longitude);
                    var content = "<table>\n" +
                        "  <tbody style=\"border:none\">\n" +
                        "    <tr><td><b>Road Name</b>: " + streetConfidenceObj.streetDetail.roadName + "</td><tr>\n" +
                        "    <tr><td><b>Road Id</b>: " + streetConfidenceObj.streetDetail.roadId + "</td><tr>\n" +
                        "    <tr><td><b>CCTV Id</b>: " + streetConfidenceObj.streetDetail.cctvId + "</td><tr>\n" +
                        "    <tr><td><b>Road Confidence</b>: " + streetConfidenceObj.score + "</td><tr>\n" +
                        "    <tr><td><a href='http://ec2-34-201-70-116.compute-1.amazonaws.com/chart-confidence.php?locationId=" + streetConfidenceObj.streetDetail.locationId + "' target='_blank'><b>History</b></a></td><tr>\n" +
                        "  </tbody>\n" +
                        "</table>";
                    var markerIcon
                    if (streetConfidenceObj.score > 85) {
                        markerIcon = greenRoadImage;
                    } else if (streetConfidenceObj.score > 70 && streetConfidenceObj.score <= 85) {
                        markerIcon = yellowRoadImage;
                    } else {
                        markerIcon = redRoadImage;
                    }
                    addmarker(latlng, String(streetConfidenceObj.score), content, markerIcon);
                });
            }
        }
    });
}

function addmarker(location, titleString, contentString, markerIcon) {
    var infoWindow = new google.maps.InfoWindow({
        content: contentString
    });
    var marker = new google.maps.Marker({
        position: location,
        title: titleString,
        draggable: false,
        map: googleMap,
        icon: markerIcon
    });
    markers.push(marker);
    marker.addListener('click', function () {
        infoWindow.open(googleMap, marker);
    });
}

function clearOverlays() {
    for (var i = 0; i < markers.length; i++) {
        markers[i].setMap(null);
    }
    markers.length = 0;
}

$(document).ready(function () {

    InitializeMap();

    $("form#upload-form").submit(function (e) {
        e.preventDefault();
        var formData = new FormData(this);
        $.ajax({
            type: 'POST',
            url: "/v1/upload",
            data: formData,
            beforeSend: function () {
                $('#spinner').show();
            },
            complete: function () {
                $('#spinner').hide();
            },
            success: function (data) {
                console.log("Upload Response -> ");
                console.log(data);
                $('#spinner').hide();
                alert("Status: " + data.status + ", Message: " + data.message + "Confidence: " + data.confidence);
                var roundedConfidenceValue = Math.ceil(data.confidence / 10) * 10;
                if (roundedConfidenceValue < 50) {
                    roundedConfidenceValue = 50;
                }
                $('#confidenceSelect').val(roundedConfidenceValue)
                getStreetConfidence(roundedConfidenceValue);
            },
            error: function (xhr, textStatus, error) {
                console.log(xhr.statusText);
                console.log(textStatus);
                console.log(error);
            },
            cache: false,
            contentType: false,
            processData: false
        });
    });

    $.ajax({
        type: "GET",
        url: "/v1/streetDetails",
        dataType: "json",
        beforeSend: function () {
            $('#spinner').show();
        },
        complete: function () {
            $('#spinner').hide();
        },
        success: function (data) {
            console.log("StreetDetails Response -> ");
            console.log(data);
            $.each(data, function (i, streetDetailObj) {
                var dropDownOption = "<option value=" + streetDetailObj.locationId + ">" + streetDetailObj.roadName + "</option>";
                $(dropDownOption).appendTo('#locationSelect');
            });
            $('#locationSelect').val(data[0].locationId)
        }
    });

    getStreetConfidence(defaultConfidence);

    $('#confidenceSelect').val(defaultConfidence)

    $("#confidenceSelect").change(function () {
        getStreetConfidence($("#confidenceSelect").val());
    });

    $('#spinner').bind('ajaxStart', function () {
        $(this).show();
    }).bind('ajaxStop', function () {
        $(this).hide();
    });
})