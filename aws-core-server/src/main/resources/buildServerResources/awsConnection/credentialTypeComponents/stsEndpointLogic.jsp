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

<script>
  let $regionSelectObject = $j('#${region_select_id}')[0];
  let $useSessionCredentialsObject = $j('#useSessionCredentialsCheckbox')[0];

  $regionSelectObject.onchange = function(){
    setStsEndpoint(this.value);
  };

  $useSessionCredentialsObject.onchange = function(){
    $j('#useSessionCredentials').val(this.checked);
    toggleStsEndpint();
  };

  $j(document).ready(function () {
    if (${empty stsEndpoint}) {
      setStsEndpoint($regionSelectObject.value);
      toggleStsEndpint();
    }
  });

  let setStsEndpoint = function (stsEndpoint) {
    $j('#${sts_endpoint_field_id}').val('https://sts.' + stsEndpoint + '.amazonaws.com')
    $j('#${sts_endpoint_field_id_iam_role}').val('https://sts.' + stsEndpoint + '.amazonaws.com')
  };

  let toggleStsEndpint = function () {
    if ($useSessionCredentialsObject.checked){
      $j('.stsEndpointClass').removeClass('hidden');
    } else {
      $j('.stsEndpointClass').addClass('hidden');
    }
  };
</script>