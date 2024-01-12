

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
    setStsEndpoint($regionSelectObject.value);
    toggleStsEndpint();
  });

  let setStsEndpoint = function (stsEndpoint) {
    $j('#${sts_endpoint_field_id}').val('https://sts.' + stsEndpoint + '.amazonaws.com');
    $j('#${sts_endpoint_field_id_iam_role}').val('https://sts.' + stsEndpoint + '.amazonaws.com');
  };

  let toggleStsEndpint = function () {
    if ($useSessionCredentialsObject.checked){
      $j('.stsEndpointClass').removeClass('hidden');
    } else {
      $j('.stsEndpointClass').addClass('hidden');
    }
  };
</script>