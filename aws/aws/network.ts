import * as aws from "@pulumi/aws";
import * as pulumi from "@pulumi/pulumi";

export class Vpc extends pulumi.ComponentResource {
  public readonly vpcId: pulumi.Output<string>;
  public readonly ecsSubnetIds: pulumi.Output<string[]>;
  public readonly apiSecurityGroupId: pulumi.Output<string>;
  public readonly vpcLinkSecurityGroupId: pulumi.Output<string>;
  public readonly batchSecurityGroupId: pulumi.Output<string>;

  constructor(pjName: string, opts?: pulumi.ComponentResourceOptions) {
    super("custom:resource:VPC", pjName, {}, opts);

    const cidrBlocks = {
      vpc: "10.20.0.0/22",
      publicAzA: "10.20.0.0/24",
      publicAzC: "10.20.1.0/24",
    };

    const vpc = new aws.ec2.Vpc(
      `${pjName}-base`,
      {
        cidrBlock: cidrBlocks.vpc,
        instanceTenancy: "default",
        enableDnsHostnames: true,
        enableDnsSupport: true,
        tags: { Name: `${pjName}-base` },
      },
      { parent: this }
    );

    const ecsSubnets = [
      new aws.ec2.Subnet(
        `${pjName}-public-1a-subnet`,
        {
          vpcId: vpc.id,
          cidrBlock: cidrBlocks.publicAzA,
          availabilityZone: "ap-northeast-1a",
          mapPublicIpOnLaunch: true,
          tags: { Name: `${pjName}-public-1a-subnet` },
        },
        { parent: this }
      ),
      new aws.ec2.Subnet(
        `${pjName}-public-1c-subnet`,
        {
          vpcId: vpc.id,
          cidrBlock: cidrBlocks.publicAzC,
          availabilityZone: "ap-northeast-1c",
          mapPublicIpOnLaunch: true,
          tags: { Name: `${pjName}-public-1c-subnet` },
        },
        { parent: this }
      ),
    ];

    const igw = new aws.ec2.InternetGateway(
      `${pjName}-igw`,
      {
        vpcId: vpc.id,
        tags: { Name: `${pjName}-igw` },
      },
      { parent: this }
    );

    const routeTable = new aws.ec2.RouteTable(
      `${pjName}-public-route-table`,
      {
        vpcId: vpc.id,
        routes: [{ cidrBlock: "0.0.0.0/0", gatewayId: igw.id }],
        tags: { Name: `${pjName}-public-route-table` },
      },
      { parent: this }
    );

    ecsSubnets.forEach((subnet, index) => {
      new aws.ec2.RouteTableAssociation(
        `${pjName}-ecs-rt-assoc-${index}`,
        {
          routeTableId: routeTable.id,
          subnetId: subnet.id,
        },
        { parent: this }
      );
    });

    const vpcLinkSecurityGroup = new aws.ec2.SecurityGroup(
      `${pjName}-vpc-link-sg`,
      {
        vpcId: vpc.id,
        tags: { Name: `${pjName}-vpc-link-sg` },
      },
      { parent: this }
    );
    this.setAllAllowEgressRule(vpcLinkSecurityGroup.id, `${pjName}-vpc-link-sg`);

    const apiSecurityGroup = new aws.ec2.SecurityGroup(
      `${pjName}-api-sg`,
      {
        vpcId: vpc.id,
        tags: { Name: `${pjName}-api-sg` },
      },
      { parent: this }
    );
    this.setAllAllowEgressRule(apiSecurityGroup.id, `${pjName}-api-sg`);

    new aws.vpc.SecurityGroupIngressRule(
      `${pjName}-api-sg-allow-http-ingress`,
      {
        securityGroupId: apiSecurityGroup.id,
        cidrIpv4: cidrBlocks.vpc,
        ipProtocol: "tcp",
        fromPort: 80,
        toPort: 80,
      },
      { parent: this }
    );

    new aws.vpc.SecurityGroupIngressRule(
      `${pjName}-api-sg-allow-vpclink-ingress`,
      {
        securityGroupId: apiSecurityGroup.id,
        referencedSecurityGroupId: vpcLinkSecurityGroup.id,
        ipProtocol: "tcp",
        fromPort: 80,
        toPort: 80,
      },
      { parent: this }
    );

    const batchSecurityGroup = new aws.ec2.SecurityGroup(
      `${pjName}-batch-sg`,
      {
        vpcId: vpc.id,
        tags: { Name: `${pjName}-batch-sg` },
      },
      { parent: this }
    );
    this.setAllAllowEgressRule(batchSecurityGroup.id, `${pjName}-batch-sg`);

    this.vpcId = vpc.id;
    this.ecsSubnetIds = pulumi.output(ecsSubnets.map((subnet) => subnet.id));
    this.apiSecurityGroupId = apiSecurityGroup.id;
    this.vpcLinkSecurityGroupId = vpcLinkSecurityGroup.id;
    this.batchSecurityGroupId = batchSecurityGroup.id;

    this.registerOutputs({});
  }

  private setAllAllowEgressRule(
    securityGroupId: pulumi.Output<string>,
    name: string
  ) {
    new aws.vpc.SecurityGroupEgressRule(
      `${name}-all-allow-egress`,
      {
        securityGroupId,
        cidrIpv4: "0.0.0.0/0",
        ipProtocol: "-1",
        fromPort: -1,
        toPort: -1,
      },
      { parent: this }
    );
  }
}
