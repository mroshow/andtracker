<!--
 Copyright (C) 2009  Axel Müller <axel.mueller@avanux.de> 
 
 This file is part of LiveTracker.
 
 LiveTracker is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 LiveTracker is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with LiveTracker.  If not, see <http://www.gnu.org/licenses/>.
-->
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@page import="de.avanux.livetracker.*"%>
<%@page import="de.avanux.livetracker.admin.*"%>
<%@page import="de.avanux.livetracker.ui.*"%>
<%@page import="java.util.UUID"%>

<%
String requestUrl = request.getRequestURL().toString();
%>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<title>Live tracker - track your Android's location live</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<script src="http://www.openlayers.org/api/OpenLayers.js"></script>
    <script src="http://www.openstreetmap.org/openlayers/OpenStreetMap.js"></script>
    <!-- this gmaps key generated for http://livetracker.dyndns.org -->
    <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=ABQIAAAAK-1v-oXS7bJWt1Z95UnNhRQTRIOvIERdrpQy89I5SlMrBS20QhRVtHc_wfzUMhH4qQuuxSC2T2CcUQ"></script>
    <link rel="shortcut icon" href="<%=requestUrl.substring(0, requestUrl.lastIndexOf('/'))%>/favicon.ico"/>
    <link rel="icon" href="<%=requestUrl.substring(0, requestUrl.lastIndexOf('/'))%>/favicon.ico"/>
    <link rel="stylesheet" type="text/css" href="<%=requestUrl.substring(0, requestUrl.lastIndexOf('/'))%>/livetracker.css" />

	<script type="text/javascript"><!--
    var map;
    var trackingID;
    var triggerUpdateActive = false;
    var refreshSeconds = 1;
    var zoom = 3;
    var center;
    var popup;
    var markerLayer;
    var locationMarker;

    OpenLayers.ProxyHost = "";

    function isUnsignedInteger(s) {
        return (s != null && s.toString().search(/^[0-9]+$/) == 0);
    }

    function createCookie(name, value) {
        document.cookie = name + '=' + value + '; path=/';
    }
    
    function readCookie(name) {
        var theCookie = "" + document.cookie;
        var ind = theCookie.indexOf(name);
        if (ind == -1 || name == "") return ""; 
        var ind1 = theCookie.indexOf(';', ind);
        if (ind1==-1) ind1 = theCookie.length; 
        return unescape(theCookie.substring(ind + name.length + 1, ind1));
    }

    function init() {
        map = new OpenLayers.Map('map', {
            projection: new OpenLayers.Projection("EPSG:900913"),
            displayProjection: new OpenLayers.Projection("EPSG:4326"),
            units: "m",
            numZoomLevels: 18,
//            maxResolution: 156543.0339,
            maxExtent: new OpenLayers.Bounds(-20037508.34, -20037508.34,
                                             20037508.34, 20037508.34),
            controls: [
                new OpenLayers.Control.Navigation(),
                new OpenLayers.Control.PanZoomBar(),
                new OpenLayers.Control.LayerSwitcher(),
                new OpenLayers.Control.Permalink(),
                new OpenLayers.Control.ScaleLine(),
            ]
        });

        var osmLayer = new OpenLayers.Layer.OSM.Mapnik("OpenStreetMap");
        var gMapLayer = new OpenLayers.Layer.Google("Google", {sphericalMercator:true});
        var gPhysicalLayer = new OpenLayers.Layer.Google("Google Physical", {type:"G_PHYSICAL_MAP", sphericalMercator:true});
        var gSatelliteLayer = new OpenLayers.Layer.Google("Google Satellite", {type:"G_SATELLITE_MAP", sphericalMercator:true});
        map.addLayers([osmLayer, gMapLayer, gPhysicalLayer, gSatelliteLayer]);

        if (isNaN(map.size.w) || isNaN(map.size.h)) {
            map.updateSize();
        }
                
        map.zoomTo(zoom);
        center = map.getCenter();

        var trackingIDFromRequest = '<%= request.getParameter(LocationMessageProvider.HTTP_PARAM_TRACKING_ID)%>';
        if(isUnsignedInteger(trackingIDFromRequest)) {
            document.getElementById('trackingID').value = trackingIDFromRequest;
            configure();        
        }
    }

    function triggerUpdate() {
        triggerUpdateActive = true;
        window.setTimeout("update()", refreshSeconds * 1000);
    }

    function update() {
        triggerUpdate();
            var request = OpenLayers.Request.GET({
                url: "http://localhost:8080/LiveTrackerServer/location?trackingID=1&trackerID=1",
                callback: parseLocation
            });
    }

    function updateLastRefresh() {
        var date = new Date();
        var minutes = ( date.getMinutes() < 10 ? '0' : '' ) + date.getMinutes();
        var seconds = ( date.getSeconds() < 10 ? '0' : '' ) + date.getSeconds();
        var time = date.getHours() + ":" + minutes + ":" + seconds;
        document.getElementById('lastRefresh').value = time;
    }
    
    function updateMap(lonlat, newZoom) {
        // center map for marker initially and if it has not been changed by user
        if(    (map.getCenter().lon == 0 &&  map.getCenter().lat == 0)
            || (map.getCenter().lon == center.lon && map.getCenter().lat == center.lat)) {                    
            map.setCenter(lonlat);
            center = map.getCenter();
        }
        
        // adjust zoom only if it has not been changed by user
    	if(map.getZoom() == zoom) {
            zoom = newZoom;
            map.zoomTo(zoom);
        }

        updateLastRefresh();
    }
    
    function parseLocation(request) {
//        alert(request.responseText);
        var singleLocationMessage = request.responseText.split(",");
        if(singleLocationMessage != null && singleLocationMessage.length == 5) {
            refreshSeconds = singleLocationMessage[3];
            
            var lonlat = new OpenLayers.LonLat(singleLocationMessage[0], singleLocationMessage[1]).transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());

            var size = new OpenLayers.Size(21,25);
            var offset = new OpenLayers.Pixel(-(size.w/2), -size.h);
            var icon = new OpenLayers.Icon('http://www.openstreetmap.org/openlayers/img/marker.png', size, offset);

            if(locationMarker != null) {
            	locationMarker.destroy();
            }
            locationMarker = new OpenLayers.Marker(lonlat, icon);

            if(markerLayer == null) {
                markerLayer = new OpenLayers.Layer.Markers("Location");
            }
            markerLayer.addMarker(locationMarker);
                
            if (popup){
                popup.destroy();
                popup = null;
            }
            popup = new OpenLayers.Popup.FramedCloud("id",
                lonlat,
                new OpenLayers.Size(200,200),
                singleLocationMessage[4],
                null,
                true);
            popup.panMapIfOutOfView = false;
            map.addPopup(popup);
            updateMap(lonlat, singleLocationMessage[2]);
        }
    }

    function configure() {
        var trackingIDValue = document.getElementById('trackingID').value;
        if((trackingIDValue != null) && isUnsignedInteger(trackingIDValue)) {
            trackingID = trackingIDValue;

            var trackerID = readCookie('trackerID');
            if(trackerID == null || trackerID == '') {
                trackerID = '<%= UUID.randomUUID()%>'
                createCookie('trackerID', trackerID);
            }
        
            if (! triggerUpdateActive) {
                update();
            }
        }
    }
    </script>
</head>
<body onload="init()">
    <table border="0" cellpadding="2">
        <tr>
            <td valign="center">
                <img src="livetracker.png" alt="LiveTracker logo" style="float:left; border:0" width="48" height="48">
            </td>
            <td width="30%" valign="center">
                <strong>Live Tracker</strong>
            </td>
            <td width="30%" valign="center">
                ID <input type="text" id="trackingID" size="5" maxlength="5" />
                   <input type="submit" onclick="configure(); return false;" value="Track" />
            </td>
            <td width="30%" valign="center">
                Last refresh <input type="text" id="lastRefresh" size="8" border="0" readonly="true" value=""/>
            </td>
        </tr>
    </table>

    <div id="mapWrapper">
        <div id="map" class="map"></div>
    </div>
</body>
</html>