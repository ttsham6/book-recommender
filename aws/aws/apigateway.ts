import * as aws from "@pulumi/aws";
import * as pulumi from "@pulumi/pulumi";

export interface ApigatewayArgs {
  lbDnsName?: pulumi.Output<string>;
  subnetIds?: pulumi.Input<pulumi.Input<string>[]>;
  securityGroupIds?: pulumi.Input<pulumi.Input<string>[]>;
  listenerArn?: pulumi.Input<pulumi.Input<string> | undefined>;
  serviceDnsName?: string;
  certificateArn?: string;
  hostedZoneId?: string;
}

export class ApiGateway extends pulumi.ComponentResource {
  constructor(
    serviceName: string,
    args: ApigatewayArgs,
    opts?: pulumi.ComponentResourceOptions
  ) {
    super("custom:resource:Apigateway", serviceName, args, opts);

    const api = new aws.apigatewayv2.Api(
      `${serviceName}-http-api`,
      {
        protocolType: "HTTP",
        description: "HTTP API for book-recommender",
        corsConfiguration: {
          allowCredentials: false,
          allowHeaders: ["authorization", "content-type"],
          allowMethods: ["GET", "OPTIONS"],
          allowOrigins: ["*"],
          maxAge: 3600,
        },
      },
      { parent: this }
    );

    const vpcLink = new aws.apigatewayv2.VpcLink(
      `${serviceName}-vpc-link`,
      {
        name: `${serviceName}-vpc-link`,
        subnetIds: args.subnetIds || [],
        securityGroupIds: args.securityGroupIds || [],
      },
      { parent: this }
    );

    const integrationUri = pulumi
      .all([args.listenerArn])
      .apply(([listenerArn]) => {
        if (listenerArn) {
          return listenerArn as string;
        }
        throw new Error("listenerArn or lbDnsName is required");
      });

    const integration = new aws.apigatewayv2.Integration(
      `${serviceName}-integration`,
      {
        apiId: api.id,
        integrationType: "HTTP_PROXY",
        integrationMethod: "ANY",
        connectionType: "VPC_LINK",
        connectionId: vpcLink.id,
        integrationUri: integrationUri as any as pulumi.Input<string>,
        payloadFormatVersion: "1.0",
      },
      { parent: this }
    );

    new aws.apigatewayv2.Route(
      `${serviceName}-default-route`,
      {
        apiId: api.id,
        routeKey: "GET /{proxy+}",
        target: pulumi.interpolate`integrations/${integration.id}`,
      },
      { parent: this }
    );

    const stage = new aws.apigatewayv2.Stage(
      `${serviceName}-stage`,
      {
        apiId: api.id,
        name: "prod",
        autoDeploy: true,
      },
      { parent: this }
    );

    if (args.serviceDnsName && args.certificateArn) {
      const domainName = new aws.apigatewayv2.DomainName(
        `${serviceName}-domain`,
        {
          domainName: `api.${args.serviceDnsName}`,
          domainNameConfiguration: {
            certificateArn: args.certificateArn,
            endpointType: "REGIONAL",
            securityPolicy: "TLS_1_2",
          },
        },
        { parent: this }
      );

      new aws.apigatewayv2.ApiMapping(
        `${serviceName}-mapping`,
        {
          apiId: api.id,
          domainName: domainName.domainName,
          stage: stage.name,
        },
        { parent: this, dependsOn: [domainName, stage] }
      );

      if (args.hostedZoneId) {
        new aws.route53.Record(
          `${serviceName}-alias-record`,
          {
            name: `api.${args.serviceDnsName}`,
            zoneId: args.hostedZoneId,
            type: "A",
            aliases: [
              {
                name: domainName.domainNameConfiguration.targetDomainName,
                zoneId: domainName.domainNameConfiguration.hostedZoneId,
                evaluateTargetHealth: false,
              },
            ],
          },
          { parent: this }
        );
      }
    }
  }
}
