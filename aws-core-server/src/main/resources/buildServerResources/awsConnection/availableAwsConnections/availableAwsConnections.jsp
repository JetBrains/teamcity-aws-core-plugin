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


<l:settingsGroup title="AWS Connection">
  <tr class="noBorder">
    <th><label for="${chosen_aws_conn_id}">Connection: <l:star/></label></th>
    <td>
        <props:selectProperty id="${avail_connections_select_id}" name="${chosen_aws_conn_id}" onchange="onAwsConnectionSelectChange()" enableFilter="true" disabled="true" className="${avail_connections_select_id}"/>
        <span class="error error_${avail_connections_select_id} hidden"></span>
    </td>
  </tr>
</l:settingsGroup>

<script type="text/javascript">

  const errorPrefix = 'error_';
  const availConnPrefix = '${avail_connections_select_id}';

  const availConnsSelectorId = BS.Util.escapeId('${avail_connections_select_id}');
  const availConnsSelector = $j(availConnsSelectorId);

  var chosenAwsConnectionId;

  onAwsConnectionSelectChange = function () {
    const selectedOption = $j('#' + '${avail_connections_select_id}' + ' option:selected');
    if (availConnsSelector.val() !== '') {
      chosenAwsConnectionId = selectedOption.val();
      console.log("SH: " + chosenAwsConnectionId)
    }
  };

  var _errorIds = [
    errorPrefix + availConnPrefix
  ];


  $j(document).ready(function () {

    function reload(selector, getValue, getLabel) {

      BS.ajaxRequest('${availableAwsConnectionsControllerUrl}', {
        parameters: '&projectId=${param.projectId}&resource=${avail_connections_rest_resource_name}',

        onComplete: function(response) {
          const json = response.responseJSON;

          const errors = json.errors;

          if(errors == null) {
            const selected = selector.val();

            selector.empty();
            if (json.length != 0) {
              json.forEach(v => {
                selector.append($j("<option></option>").attr("value", getValue(v)).text(getLabel(v)));

                if(getValue(v) === chosenAwsConnectionId){
                  console.log(getValue(v));
                  selector.value = chosenAwsConnectionId;
                }
              });
              selector.prop('disabled', false);
              selector.val(selected).change();
              BS.enableJQueryDropDownFilter(selector.attr('id'), {});
              toggleErrors(false);


            } else {
              addError(
                'There are no available AWS connections.<br>\
                <span class="smallNote">To configure connections, use the <a href="<c:url value='/admin/editProject.html?projectId=${param.projectId}&tab=oauthConnections#addDialog=${connectorType}'/>" target="_blank" rel="noreferrer">Project Connections</a> page</span>',
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
    }

    function getAvailableAwsConnections() {
      reload(availConnsSelector, v => v.first, v => `\${v.second}`)
    }

    getAvailableAwsConnections();
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