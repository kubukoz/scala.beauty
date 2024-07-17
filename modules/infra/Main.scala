import besom.*
import besom.api.aws
import besom.api.aws.ec2
import besom.api.aws.ec2.Vpc
import besom.api.aws.ec2.VpcArgs
import besom.api.aws.ecr
import besom.api.aws.ecr.Repository
import besom.api.aws.ecs
import besom.api.aws.ecs.inputs.ServiceLoadBalancerArgs
import besom.api.aws.ecs.inputs.TaskDefinitionRuntimePlatformArgs
import besom.api.awsx.ecr as ecrx
import besom.api.awsx.ecs.inputs.FargateServiceTaskDefinitionArgs
import besom.api.awsx.ecs.inputs.TaskDefinitionContainerDefinitionArgs
import besom.api.awsx.ecs.FargateTaskDefinition
import besom.api.awsx.ecs.FargateTaskDefinitionArgs
import besom.api.awsx.ecs as ecsx
import besom.api.awsx.lb as lbx

@main def main = Pulumi.run {

  inline def n(inline s: String) = s"scala-beauty-$s"

  // val vpc = ec2.Vpc(
  //   n("vpc"),
  //   ec2.VpcArgs(
  //     cidrBlock = "10.0.0.0/16"
  //   ),
  // )

  val lb = lbx.ApplicationLoadBalancer(n("lb"))

  val cluster = ecs.Cluster(
    n("cluster"),
    ecs.ClusterArgs(
    ),
  )

  val repo = ecr.Repository(n("repo"), ecr.RepositoryArgs())

  val image = ecrx.Image(
    n("image"),
    ecrx.ImageArgs(
      repositoryUrl = repo.repositoryUrl,
      context = "build",
      dockerfile = "build/Dockerfile",
      platform = "linux/arm64",
    ),
  )

  val service = ecsx.FargateService(
    "service",
    ecsx.FargateServiceArgs(
      cluster = cluster.arn,
      assignPublicIp = true,
      desiredCount = 1,
      taskDefinitionArgs = FargateServiceTaskDefinitionArgs(
        container = TaskDefinitionContainerDefinitionArgs(
          name = n("container"),
          image = image.imageUri,
          cpu = 512,
          memory = 1024,
          essential = true,
          portMappings = List(
            // ecsx.inputs.TaskDefinitionPortMappingArgs(
            // containerPort = 8080
            // hostPort = 8080,
            // protocol = "tcp",
            // https://github.com/VirtusLab/besom/issues/398
            // targetGroup = lb.defaultTargetGroup,
            // )
          ),
        ),
        runtimePlatform = TaskDefinitionRuntimePlatformArgs(
          cpuArchitecture = "ARM64"
        ),
      ),
      loadBalancers = List(
        ServiceLoadBalancerArgs(
          containerName = n("container"),
          containerPort = 80,
          targetGroupArn = lb.defaultTargetGroup.arn,
        )
      ),
    ),
  )

  Stack(service).exports(
    appUrl = p"http://${lb.loadBalancer.dnsName}"
  )
}
