<%--
  ~ Copyright 2000-2022 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<%@include file="awsConnectionConstants.jspf"%>

<c:url var="availableAwsConnectionsControllerUrl" value="${avail_connections_controller_url}"/>

<props:hiddenProperty id="chosenAwsConnIdParam" name="${chosen_aws_conn_id}"/>

<c:choose>
  <c:when test="${intprop:getBooleanOrTrue(aws_feature_prop_name)}">
    <table class="runnerFormTable">
      <tr class="noBorder">
        <th>
          <label for="availAwsConnectionsSelect">Select AWS Connection:</label>
        </th>
        <td>
          <select id="availAwsConnectionsSelect" onchange="onAwsConnectionSelectChange()" class= "availAwsConnections hidden"> </select>
          <span class="error error_availAwsConnections hidden"></span>
        </td>
      </tr>
    </table>
  </c:when>
  <c:otherwise>
    AWS Connections feature is turned off
  </c:otherwise>
</c:choose>

<script type="text/javascript">

  const errorPrefix = 'error_';
  const availConnPrefix = 'availAwsConnections';
  const availConnsSelectId = availConnPrefix+'Select';

  const chosenAwsConnParamId = 'chosenAwsConnIdParam';

  var _errorIds = [
    errorPrefix + availConnPrefix
  ];


  $j(document).ready(function() {
    getAvailableAwsConnections();
  });

  var onAwsConnectionSelectChange = function () {
    const selectedOption = $j('#' + availConnsSelectId + ' option:selected');
    if (selectedOption.val() !== '') {
      $j('#' + chosenAwsConnParamId).val(selectedOption.val());
    }
  };

  var getAvailableAwsConnections = function () {
    const availConnsSelectElement = $j('#' + availConnsSelectId);
    BS.ajaxRequest('${availableAwsConnectionsControllerUrl}', {

      parameters: '&projectId=${param.projectId}',

      onComplete: function(response) {

        const errors = response.responseXML.documentElement.getElementsByTagName('error');
        if (errors.length == 0) {

          const connections = response.responseXML.documentElement.getElementsByTagName('${aws_conn_element_name}');
          if (connections.length > 0) {
            availConnsSelectElement.empty();

            const select = document.getElementById(availConnsSelectId);
            for (let i = 0; i < connections.length; i++) {
              const option = document.createElement('option');
              option.text = connections[i].getAttribute('${aws_conn_attr_name}');
              option.value = connections[i].getAttribute('${aws_conn_attr_id}');
              select.add(option);
            }
            toggleAvailableConnectionsSelect(true);
            toggleErrors(false)
          } else {
            addError('There are no available AWS connections, please, create one in the Project configuration.', $j('.' + errorPrefix + availConnPrefix));
            toggleErrors(true);
            toggleAvailableConnectionsSelect(false);
          }

        } else {
          for (let i = 0; i < errors.length; i++) {
            addError(errors[i].childNodes[0].nodeValue, $j('.' + errors[i].getAttribute('id')));
          }
          toggleErrors(true);
        }
      }
    });
  };

  var toggleAvailableConnectionsSelect = function (show){
    if(show)
      $j('.' + availConnPrefix).removeClass('hidden');
    else
      $j('.' + availConnPrefix).addClass('hidden');
  };

  var toggleErrors = function (show){
    _errorIds.forEach(errorId => {
      if(show)
        $j('.' + errorId).removeClass('hidden');
      else
        $j('.' + errorId).addClass('hidden');
    });
  };


  var addError = function (errorHTML, target) {
    target.append($j('<div>').text(errorHTML));
  };

  var clearAllErrors = function (){
    _errorIds.forEach(errorId => {
      clearError(errorId)
    })
  };

  var clearError = function(errorId) {
    var target = $j('.error_' + errorId);
    target.empty();
  };

</script>