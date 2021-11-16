[![Archived header](https://github.com/newrelic/opensource-website/raw/main/src/images/categories/Archived.png)](https://opensource.newrelic.com/oss-category/#archived)


# cloudformation-partner-integration
An early example of how one might integrate New Relic Alerts with AWS Cloudformation

# Legacy public doc

New Relic’s [AWS CloudFormation](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/Welcome.html) integration allows you to add alert conditions to new or existing CloudFormation stacks using the [New Relic alerts](/docs/alerts/new-relic-alerts/getting-started) resource provider. This document explains how to activate and use this integration.

## Features

[AWS CloudFormation](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/Welcome.html) is an Amazon Web Services (AWS) service that allows you to use programming languages or a simple text file to model and provision, in an automated and secure manner, all the resources needed for your applications across all regions and accounts. It allows you to simply create and duplicate a collection of AWS resources, known as a [stack](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/stacks.html). Whenever a stack is generated, AWS CloudFormation provisions the resources that are specified in your [template](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cfn-whatis-concepts.html#w2ab1b5c15b7).

Using the CloudFormation integration for New Relic, you can add [alert conditions](/docs/alerts/new-relic-alerts-beta/configuring-alert-policies/define-alert-conditions) to your CloudFormation template using our custom resource provider, giving you the ability to monitor your infrastructure and applications with [New Relic Alerts](/docs/alerts/new-relic-alerts/getting-started).

## Requirements

To use the Amazon CloudFormation integration, ensure your system meets these requirements:

* [AWS Command Line Interface](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) (AWS CLI) installed.
* [New Relic REST API key](/docs/alerts/rest-api-alerts/new-relic-alerts-rest-api/rest-api-calls-new-relic-alerts).
* Optional: [CloudFormation CLI](https://github.com/aws-cloudformation/aws-cloudformation-rpdk) installed.

## Install the CloudFormation integration [#install]

To install the CloudFormation integration:

1. Navigate to the New Relic [CloudFormation Partner Integration](https://github.com/newrelic/cloudformation-partner-integration) GitHub repository.
2. From the repository page, [clone or download the repository](https://help.github.com/en/articles/cloning-a-repository).

## Register the resource provider [#register]

To use private resource providers you must first register them with CloudFormation, in the accounts and regions in which you want to use them. Once you're registered a resource provider, it will appear in the CloudFormation registry for that account and region, and you can use it in your stack templates.

You can register the resource provider using one of the following methods:

<CollapserGroup>
  <Collapser
    id="cloudformation-api"
    title="Register using the CloudFormation API"
  >
    To register the resource provider using the CloudFormation API:

    1. In your terminal or command-line interface, run the [`RegisterType` action](https://docs.aws.amazon.com/AWSCloudFormation/latest/APIReference/API_RegisterType.html) to validate, package, and upload the resource provider to the CloudFormation Registry:

       ```
       aws cloudformation register-type --type-name NewRelic::Alerts::NrqlAlert --schema-handler-package s3://nr-cloudformation-downloads/newrelic-alerts-nrqlalert.zip --type RESOURCE
       ```

    **Note:** If you are updating a previously registered resource, you can use the returned registration token to track the progress of the registration request using the [`DescribeTypeRegistration` action](https://docs.aws.amazon.com/AWSCloudFormation/latest/APIReference/API_DescribeTypeRegistration.html) in the CloudFormation API.

    Token example:

    ```
    Registration in progress with token: <3c27b9e6-dca4-4892-ba4e-3c0example>
    ```
  </Collapser>

  <Collapser
    id="cloudformation-cli"
    title="Register using the CloudFormation CLI"
  >
    To register the resource provider using the CloudFormation Command Line Interface (CLI):

    1. Refer to the [AWS CloudFormation Github README](https://github.com/aws-cloudformation/aws-cloudformation-rpdk) for instructions on downloading and installing the CloudFormation CLI.
    2. Once you have installed the CloudFormation CLI, run the following command to validate, package, and upload the resource provider to the CloudFormation Registry:

       ```
       cfn submit -v --region <region>
       ```

    **Note:** If you are updating a previously registered resource, you can use the returned registration token to track the progress of the registration request using the [`DescribeTypeRegistration` action](https://docs.aws.amazon.com/AWSCloudFormation/latest/APIReference/API_DescribeTypeRegistration.html) in the CloudFormation API.

    Token example:

    ```
    Validating your resource specification...
    Packaging Java project
    Creating managed upload infrastructure stack
    Managed upload infrastructure stack already exists. Attempting to update
    Managed upload infrastructure stack is up to date
    Registration in progress with token: <3c27b9e6-dca4-4892-ba4e-3c0example>
    ```
  </Collapser>
</CollapserGroup>

## Configure the resource provider [#configure]

Once you have [registered](#register) the resource, add the `NewRelic::Alerts::NrqlAlert` [resource](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-template-resource-type-ref.html) to the JSON or YAML file for your CloudFormation stack, using the following properties to configure the alert condition. For an example configuration, see the [Lambda CloudFormation resource examples.](#example-config)

<table>
  <thead>
    <tr>
      <th style={{ width: "150px" }}>
        Properties
      </th>

      <th style={{ width: "175px" }}/>

      <th>
        Description
      </th>

      <th style={{ width: "100px" }}>
        Type
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        `ApiKey`
      </td>

      <td/>

      <td>
        The [New Relic REST API key](/docs/alerts/rest-api-alerts/new-relic-alerts-rest-api/rest-api-calls-new-relic-alerts).

        **Required**.
      </td>

      <td>
        string
      </td>
    </tr>

    <tr>
      <td>
        `PolicyId`
      </td>

      <td/>

      <td>
        The unique ID for the alert policy's account ID associated with the condition; for example, `1234567890`.

        **Required**.
      </td>

      <td>
        integer
      </td>
    </tr>

    <tr>
      <td>
        `Condition`
      </td>

      <td>
        `Name`
      </td>

      <td>
        The name of the alerting condition.
      </td>

      <td>
        string
      </td>
    </tr>

    <tr>
      <td/>

      <td>
        `RunbookUrl`
      </td>

      <td>
        Link to runbook for resolving this error.
      </td>

      <td>
        string
      </td>
    </tr>

    <tr>
      <td/>

      <td>
        `Enabled`
      </td>

      <td>
        The status of your alert condition. `true` if the alert is active. Default: `false`
      </td>

      <td>
        boolean
      </td>
    </tr>

    <tr>
      <td/>

      <td>
        `ExpectedGroups`
      </td>

      <td>
        This is the number of groups you expect to see at any given time. It is used in combination with the [`ignore_overlap`](/docs/alerts/rest-api-alerts/new-relic-alerts-rest-api/alerts-conditions-api-field-names#ignore_overlap) option.
      </td>

      <td>
        integer
      </td>
    </tr>

    <tr>
      <td/>

      <td>
        `IgnoreOverlap`
      </td>

      <td>
        If disabled, New Relic looks for a convergence of groups. If the condition is looking for 2 or more groups, and the returned values cannot be separated into that number of distinct groups, then that will also produce a violation. This type of overlap event is represented on a chart by group bands touching.
      </td>

      <td>
        boolean
      </td>
    </tr>

    <tr>
      <td/>

      <td>
        `ValueFunction`
      </td>

      <td>
        This is the value function used from the plugin metric, and be one of the following strings:

        * min
        * max
        * average
        * sample_size
        * total
        * percent

        For more information on `ValueFunction`, see [Alerts conditions API glossary: value_function](/docs/alerts/rest-api-alerts/new-relic-alerts-rest-api/alerts-conditions-api-field-names#value_function).
      </td>

      <td>
        string
      </td>
    </tr>

    <tr>
      <td/>

      <td>
        `Terms`
      </td>

      <td>
        An array of key/value pairs that may include the following:

        * `Duration`
        * `Operator`
        * `Priority`
        * `Threshold`
        * `TimeFunction`

        For detailed field definitions, see [Alerts conditions API glossary: terms](/docs/alerts/rest-api-alerts/new-relic-alerts-rest-api/alerts-conditions-api-field-names#terms_duration).
      </td>

      <td>
        array
      </td>
    </tr>

    <tr>
      <td/>

      <td>
        `Nrql`
      </td>

      <td>
        The NRQL query being monitored by Alerts, must include both of the following:

        * `Query` (**Required**)
        * `SinceValue` (**Required**)

        For detailed field definitions, see [Alerts conditions API glossary: nrql](/docs/alerts/rest-api-alerts/new-relic-alerts-rest-api/alerts-conditions-api-field-names#nrql-query).

        **Required**.
      </td>

      <td>
        string
      </td>
    </tr>
  </tbody>
</table>

### Example configuration [#example-config]

Lambda CloudFormation resource examples:

<CollapserGroup>
  <Collapser
    id="json-example"
    title="JSON example configuration"
  >
    ```
    {
    "AWSTemplateFormatVersion": "2010-09-09",
    "Resources": {
        "LambdaNodeAlert": {
          "Type": "NewRelic::Alerts::NrqlAlert",
          "Properties": {
            "ApiKey": "YOUR_API_KEY",
            "PolicyId": YOUR_POLICY_ID,
            "NrqlCondition": {
              "Name": "Alert Condition NAme",
              "RunbookUrl": "http://example.com/runbook.html",
              "Enabled": false,
              "ExpectedGroups": 1,
              "IgnoreOverlap": true,
              "Terms": [
                {
                  "Duration": 1,
                  "Operator": "equal",
                  "Priority": "critical",
                  "Threshold": 1.0,
                  "TimeFunction": "all"
                }
              ],
              "Nrql": {
                "Query": "SELECT count(*) FROM AwsLambdaInvocationError FACET provider.functionName",
                "SinceValue": 1
              }
            }
          }
        }
    }
    ```
  </Collapser>

  <Collapser
    id="yaml-example"
    title="YAML example configuration"
  >
    ```
    AWSTemplateFormatVersion: 2010-09-09
    Resources:
      # Here's our custom resource type, which creates an alert in New Relic that triggers when the function is invoked
      LambdaNodeAlert:
        Type: NewRelic::Alerts::NrqlAlert
        Properties:
          #TODO: Your values here
          ApiKey: YOUR_API_KEY
          PolicyId: YOUR_POLICY_ID
          NrqlCondition:
            Name: Alert Condition Test
            RunbookUrl: http://example.com/runbook
            Enabled: true
            ExpectedGroups: 1
            IgnoreOverlap: true
            Terms:
              - Duration: 1
                Operator: "equal"
                Priority: "critical"
                Threshold: 1.0
                TimeFunction: "all"
            Nrql:
              Query: "SELECT count(*) FROM AwsLambdaInvocationError FACET provider.functionName"
              SinceValue: 1
    ```
  </Collapser>
</CollapserGroup>

## Provision the Resource in a CloudFormation Stack [#create-alert-condition]

To use the resource provider to provision your stack and create an alert condition:

1. Once you have [added the resource provider](#configure) to your CloudFormation template, run the following command in your terminal or command-line interface to provision the resource and create your CloudFormation stack:

   ```
   aws cloudformation create-stack --region us-west-2 \
   --template-body "file://stack.yaml" \
   --stack-name NewRelicAlert
   ```
2. To view your alert, go to [**alerts.newrelic.com**](https://alerts.newrelic.com), select **Alert policies > (selected policy) > Alert conditions**.

## What's next?

To learn more about using alerts:

* Check out the New Relic University tutorial [Intro to alert policies](https://newrelic.wistia.com/medias/2u6nnm6epc). Or, go directly to the full online course [New Relic alerting](https://learn.newrelic.com/alerting-course).
* Read [Alerts best practices](/docs/alerts/new-relic-alerts/getting-started/best-practices-alert-policies).
* Learn about the [New Relic Alerts API](/docs/alerts/rest-api-alerts/new-relic-alerts-rest-api/rest-api-calls-new-relic-alerts).
