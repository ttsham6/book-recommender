import * as aws from "@pulumi/aws";
import * as awsx from "@pulumi/awsx";
import * as pulumi from "@pulumi/pulumi";

export interface ClusterArgs {
  subnetIds: pulumi.Output<string[]>;
  containerSgId: pulumi.Output<string>;
  taskRoleArn: pulumi.Output<string>;
  executionRoleArn: pulumi.Output<string>;
  contextPath: string;
  environments: { name: string; value: pulumi.Input<string> }[];
  memory?: number;
  cpu?: number;
  imageTag?: string;
}

export class ApiService extends pulumi.ComponentResource {
  public readonly loadBalancerDnsName: pulumi.Output<string>;
  public readonly loadBalancerListenerArn: pulumi.Output<pulumi.Input<string> | undefined>;

  constructor(
    appName: string,
    args: ClusterArgs,
    opts?: pulumi.ComponentResourceOptions
  ) {
    super(`custom:resource:${appName}-api`, appName, args, opts);

    const cluster = new aws.ecs.Cluster(
      `${appName}-cluster`,
      {},
      { parent: this }
    );

    const loadBalancer = new awsx.lb.NetworkLoadBalancer(
      `${appName}-nlb`,
      {
        name: `${appName}-nlb`,
        subnetIds: args.subnetIds,
        defaultTargetGroup: {
          protocol: "TCP",
          port: 80,
          targetType: "ip",
        },
        internal: true,
      },
      { parent: this }
    );

    const repo = new awsx.ecr.Repository(
      `${appName}-repo`,
      {},
      { parent: this }
    );

    const image = new awsx.ecr.Image(
      `${appName}-image`,
      {
        repositoryUrl: repo.url,
        context: args.contextPath,
        imageTag: args.imageTag || "latest",
        platform: "linux/amd64",
      },
      { parent: this }
    );

    const service = new awsx.ecs.FargateService(`${appName}-service`, {
      cluster: cluster.arn,
      taskDefinitionArgs: {
        container: {
          name: "app",
          image: image.imageUri,
          cpu: args.cpu || 256,
          memory: args.memory || 512,
          essential: true,
          portMappings: [
            {
              containerPort: 80,
              hostPort: 80,
              protocol: "tcp",
              targetGroup: loadBalancer.defaultTargetGroup,
            },
          ],
          environment: args.environments.concat([
            {
              name: "JAVA_OPTS",
              value:
                "-Xmx256m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof",
            },
          ]),
        },
        executionRole: {
          roleArn: args.executionRoleArn,
        },
        taskRole: {
          roleArn: args.taskRoleArn,
        },
      },
      networkConfiguration: {
        assignPublicIp: true,
        subnets: args.subnetIds,
        securityGroups: [args.containerSgId],
      },
      desiredCount: 1,
    });

    const scalableTarget = new aws.appautoscaling.Target(
      `${appName}-scalable-target`,
      {
        maxCapacity: 1,
        minCapacity: 1,
        resourceId: pulumi.interpolate`service/${cluster.name}/${service.service.name}`,
        scalableDimension: "ecs:service:DesiredCount",
        serviceNamespace: "ecs",
      },
      { parent: this, dependsOn: [service.service] }
    );

    new aws.appautoscaling.ScheduledAction(
      `${appName}-night-stop`,
      {
        name: `${appName}-night-stop`,
        serviceNamespace: "ecs",
        schedule: "cron(0 14 ? * FRI,SAT *)", // JST 23:00 UTC on Friday and Saturday
        scalableDimension: "ecs:service:DesiredCount",
        resourceId: pulumi.interpolate`service/${cluster.name}/${service.service.name}`,
        scalableTargetAction: { minCapacity: 0, maxCapacity: 0 },
      },
      { parent: this, dependsOn: [scalableTarget] }
    );

    new aws.appautoscaling.ScheduledAction(
      `${appName}-morning-start`,
      {
        name: `${appName}-morning-start`,
        serviceNamespace: "ecs",
        schedule: "cron(0 23 ? * FRI,SAT *)", // JST 8:00 AM on Saturday and Sunday
        scalableDimension: "ecs:service:DesiredCount",
        resourceId: pulumi.interpolate`service/${cluster.name}/${service.service.name}`,
        scalableTargetAction: { minCapacity: 1, maxCapacity: 1 },
      },
      { parent: this, dependsOn: [scalableTarget] }
    );

    this.loadBalancerDnsName = loadBalancer.loadBalancer.dnsName;
    this.loadBalancerListenerArn = pulumi
      .output(loadBalancer.listeners)
      .apply((listeners) => listeners?.[0])
      .apply((listener) => listener?.arn);
  }
}
