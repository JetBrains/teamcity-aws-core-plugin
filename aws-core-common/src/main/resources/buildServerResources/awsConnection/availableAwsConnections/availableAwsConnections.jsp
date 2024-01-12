

<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<%@include file="../awsConnectionConstants.jspf"%>
<%@include file="../sessionCredentials/sessionCredentialsConst.jspf"%>
<%@include file="../credentialTypeComponents/accessKeys/awsAccessKeysCredsConstants.jspf"%>

<c:url var="availableAwsConnectionsControllerUrl" value="${avail_connections_controller_url}"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<c:set var="previouslyChosenAwsConnId" value="${propertiesBean.properties[chosen_aws_conn_id]}"/>
<c:set var="isUsingSessionCredentials" value="${(empty propertiesBean.properties[use_session_credentials_param]) ? 'true' : propertiesBean.properties[use_session_credentials_param]}"/>
<c:set var="awsCredsType" value="${propertiesBean.properties[aws_creds_type_param]}"/>

<tr class="noBorder">
  <th><label for="${chosen_aws_conn_id}">${chosen_aws_conn_label}: <l:star/></label></th>
  <td>
    <props:selectProperty id="${avail_connections_select_id}" name="${chosen_aws_conn_id}" enableFilter="true" disabled="true" className="${avail_connections_select_id}"/>
    <span class="error error_${avail_connections_select_id} hidden"></span>
    <span class="error" id="error_${chosen_aws_conn_id}" style="word-break: break-all;"></span>
  </td>
</tr>

<jsp:include page="../sessionCredentials/sessionCredentialsConfig.jsp"/>

<script type="text/javascript">

  const errorPrefix = 'error_';
  const availConnPrefix = '${avail_connections_select_id}';

  const availConnsSelectorId = BS.Util.escapeId('${avail_connections_select_id}');
  const availConnsSelector = $j(availConnsSelectorId);
  const $availConnsSelectorObject = $j(availConnsSelectorId)[0];

  const awsConnectionsMap = new Map();

  const _errorIds = [
    errorPrefix + availConnPrefix
  ];

  let sessionCredentialsMuted = true;


  availConnsSelector.on('change', function () {
    toggleSessionDurationField();
  });

  $j(document).ready(function () {

    if ('${empty param.sessionDuration}' === 'true') {
      sessionCredentialsMuted = false;
    }

    BS.ajaxRequest('${availableAwsConnectionsControllerUrl}', {
      parameters: '&projectId=${param.projectId}&resource=${avail_connections_rest_resource_name}&${allowed_in_builds_request_param}=${param.forBuildStep}&${principal_aws_conn_id}=${param.principalAwsConnId}',

      onComplete: function(response) {

        const json = response.responseJSON;
        const errors = json.errors;

        if(errors == null) {
          availConnsSelector.empty();

          if (json.length !== 0) {

            availConnsSelector.append(
              $j("<option></option>")
              .attr("value", "${unselected_principal_aws_connection_value}")
              .text(`-- Choose the Principal AWS Connection --`)
            );

            json.forEach(
              awsConnectionProps => {
                availConnsSelector.append(
                  $j("<option></option>")
                  .attr("value", awsConnectionProps[0])
                  .text(`\${awsConnectionProps[1]}`)
                );

                awsConnectionsMap.set(awsConnectionProps[0], {
                  "name": awsConnectionProps[1],
                  "isUsingSessionCredentials": awsConnectionProps[2]
                })
              }
            );
            availConnsSelector.prop('disabled', false);

            let previouslySelectedOptionIndex = json.findIndex(awsConnectionProps => awsConnectionProps[0] === '${previouslyChosenAwsConnId}');
            if(previouslySelectedOptionIndex !== -1){
              previouslySelectedOptionIndex++;// add one for the unselected option
              let newSelector = $(availConnsSelector.attr('id'));
              newSelector.selectedIndex = previouslySelectedOptionIndex;
            }

            BS.jQueryDropdown(availConnsSelector).ufd("changeOptions");
            toggleErrors(false);
            toggleSessionDurationField();

          } else {
            addError(
              'There are no available AWS connections.<br>\
              <span class="smallNote">Create an <a href="<c:url value='/admin/editProject.html?projectId=${param.projectId}&tab=oauthConnections#addDialog=${aws_connection_type}'/>" target="_blank" rel="noreferrer">AWS Connection</a> first</span>',
              $j('.' + errorPrefix + availConnPrefix)
            );
            toggleErrors(true);
          }

        } else {
            errors.forEach(({message, id}) => addError(message, $j('.' + id)));
          toggleErrors(true);
        }
      }
    });

    BS.enableJQueryDropDownFilter(availConnsSelector.attr('id'), {});
  });

  let toggleSessionDurationField = function () {
    var sessionDurationParam = document.getElementById('${session_duration_param}_row');

    if (sessionCredentialsMuted) {
      sessionDurationParam.classList.add("hidden");
      return;
    }

    const selectedConnectionId = document.getElementById('${avail_connections_select_id}').value;
    if("${unselected_principal_aws_connection_value}" == selectedConnectionId) {
      sessionDurationParam.classList.add("hidden");
      return;
    }

    const awsConnection = awsConnectionsMap.get(selectedConnectionId);
    if (awsConnection.isUsingSessionCredentials == 'false') {
      sessionDurationParam.classList.add("hidden");
    } else {
      sessionDurationParam.classList.remove("hidden");
    }
  };

  let toggleErrors = function (showErrors){
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


  let addError = function (errorHTML, target) {
    target.append($j('<div>').html(errorHTML));
  };

  const clearAllErrors = function () {
    _errorIds.forEach(errorId => {
      clearError(errorId)
    })
  };

  let clearError = function(errorId) {
    const target = $j('.error_' + errorId);
    target.empty();
  };
</script>