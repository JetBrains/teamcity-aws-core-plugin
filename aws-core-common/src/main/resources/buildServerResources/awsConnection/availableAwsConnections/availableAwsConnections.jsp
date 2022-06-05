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

<%@include file="../awsConnectionConstants.jspf"%>

<c:url var="availableAwsConnectionsControllerUrl" value="${avail_connections_controller_url}"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<c:set var="previouslyChosenAwsConnId" value="${propertiesBean.properties[chosen_aws_conn_id]}"/>

<tr class="noBorder">
  <th><label for="${chosen_aws_conn_id}">Connection: <l:star/></label></th>
  <td>
    <props:selectProperty id="${avail_connections_select_id}" name="${chosen_aws_conn_id}" enableFilter="true" disabled="true" className="${avail_connections_select_id}"/>
    <span class="error error_${avail_connections_select_id} hidden"></span>
  </td>
</tr>

<c:choose>
  <c:when test = "${param.configurableSessionDuration == 'true'}">
    <jsp:include page="../sessionCredentials/sessionCredentialsConfig.jsp"/>
  </c:when>

  <c:otherwise/>
</c:choose>

<script type="text/javascript">

  const errorPrefix = 'error_';
  const availConnPrefix = '${avail_connections_select_id}';

  const availConnsSelectorId = BS.Util.escapeId('${avail_connections_select_id}');
  const availConnsSelector = $j(availConnsSelectorId);

  var _errorIds = [
    errorPrefix + availConnPrefix
  ];


  $j(document).ready(function () {
    BS.ajaxRequest('${availableAwsConnectionsControllerUrl}', {
      parameters: '&projectId=${param.projectId}&resource=${avail_connections_rest_resource_name}',

      onComplete: function(response) {

        const json = response.responseJSON;
        const errors = json.errors;

        if(errors == null) {
          availConnsSelector.empty();

          if (json.length != 0) {
            json.forEach(
              connectionNameIdPair => {
                availConnsSelector.append(
                  $j("<option></option>")
                  .attr("value", connectionNameIdPair.first)
                  .text(`\${connectionNameIdPair.second}`)
                );
              }
            );
            availConnsSelector.prop('disabled', false);

            const previouslySelectedOptionIndex = json.findIndex(option => option.first === '${previouslyChosenAwsConnId}');
            if(previouslySelectedOptionIndex != -1){
              let newSelector = $(availConnsSelector.attr('id'));
              newSelector.selectedIndex = previouslySelectedOptionIndex;
            }

            BS.jQueryDropdown(availConnsSelector).ufd("changeOptions");
            toggleErrors(false);

          } else {
            addError(
              'There are no available AWS connections.<br>\
              <span class="smallNote">To configure connections, use the <a href="<c:url value='/admin/editProject.html?projectId=${param.projectId}&tab=oauthConnections#addDialog=${aws_connection_type}'/>" target="_blank" rel="noreferrer">Project Connections</a> page</span>',
              $j('.' + errorPrefix + availConnPrefix)
            );
            toggleErrors(true);
          }

        } else {
          for (let i = 0; i < errors.length; i++) {
            errors.forEach(({message, id}) => addError(message, $j('.' + id)));
          }
          toggleErrors(true);
        }
      }
    });

    BS.enableJQueryDropDownFilter(availConnsSelector.attr('id'), {});
  });

  var toggleErrors = function (showErrors){
    _errorIds.forEach(errorId => {
      if(showErrors)
        $j('.' + errorId).removeClass('hidden');
      else
        $j('.' + errorId).addClass('hidden');
    });

    if(showErrors)
      $j('.' + availConnPrefix).addClass('hidden');
    else
      $j('.' + availConnPrefix).removeClass('hidden');
  };


  var addError = function (errorHTML, target) {
    target.append($j('<div>').html(errorHTML));
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