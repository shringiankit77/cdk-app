package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class DataBaseandECSStack extends Stack {
    private String databasePassword = "Elegant#123";
    private String vpcName = "DemoAppVpc";
    private String clusterName = "DemoAppCluster";
    private String ecsTaskRoleName = "ECSTaskDemoExecutionRole";
    private String databaseName = "assingment_db";
    private String parameterBaseString = "/app/config/demo/";
    private String securityGroupName = "MySqlDemoSecurityGroup";
    private String databaseInstanceName = "FirstDemoDataBase";
    private String repositoryName = "springboot-application-demo";
    private String logGroupName = "DemoAppLogGroup";
    private String secractGroupName = "DemoSecratGroup";
    private String ecrRepoId = "SpringBootRepository";
    private String fargateServiceId = "FargateService";
    public DataBaseandECSStack(final Construct scope, final String id, StackProps props){
        super(scope,id);

        // ✅ VPC with public and private subnets
        // ✅ 1. Create a VPC with public and private subnets
        Vpc vpc = Vpc.Builder.create(this, vpcName)
                .maxAzs(2) // Spread across 2 Availability Zones
                .natGateways(1)
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .cidrMask(24)
                                .name("public-subnet")
                                .subnetType(SubnetType.PUBLIC)
                                .build(),
                        SubnetConfiguration.builder()
                                .cidrMask(24)
                                .name("private-subnet")
                                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                                .build()
                ))
                .build();


        // ✅ ECS Cluster
        Cluster cluster = Cluster.Builder.create(this, clusterName)
                .vpc(vpc)
                .build();


        // ✅ IAM Role for ECS task execution
        Role executionRole = Role.Builder.create(this, ecsTaskRoleName)
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .managedPolicies(java.util.List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("service-role/AmazonECSTaskExecutionRolePolicy")
                ))
                .build();

        // ✅ Log group
        LogGroup logGroup = LogGroup.Builder.create(this, logGroupName)
                .retention(RetentionDays.ONE_WEEK)
                .build();

        // ✅ MySQL credentials
        Secret dbCredentials = Secret.Builder.create(this, secractGroupName)
                .secretName("AppDBCredentials1")
                .generateSecretString(SecretStringGenerator.builder()
                        .secretStringTemplate("{\"username\":\"admin\",\"password\" : \""+databasePassword+"\"}")
                        .generateStringKey(databasePassword)
                        .excludePunctuation(true)
                        .build())
                .build();


        // ✅ Security group for RDS
        SecurityGroup dbSG = SecurityGroup.Builder.create(this, securityGroupName)
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();

        // ✅ RDS MySQL
        DatabaseInstance db = DatabaseInstance.Builder.create(this, databaseInstanceName)
                .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
                        .version(MysqlEngineVersion.VER_8_0_34)
                        .build()))
                .vpc(vpc)
                .credentials(Credentials.fromSecret(dbCredentials))
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE_WITH_EGRESS).build())
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                .allocatedStorage(20)
                .securityGroups(java.util.List.of(dbSG))
                .databaseName(databaseName)
                .publiclyAccessible(false)
                .build();


        // ✅ Security group for ECS
        SecurityGroup ecsSG = SecurityGroup.Builder.create(this, "ECSSG1")
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();

        // ✅ Allow ECS to access MySQL
        dbSG.addIngressRule(ecsSG, Port.tcp(3306), "Allow ECS access to MySQL");


        // ✅ ECR repository
        IRepository     ecrRepo =  Repository.fromRepositoryName(this,ecrRepoId,repositoryName);
                 if(ecrRepo==null) {
                  ecrRepo=   Repository.Builder.create(this, ecrRepoId)
                             .repositoryName(repositoryName)
                             .build();
                 }





        // ✅ Fargate Service with Load Balancer
        ApplicationLoadBalancedFargateService fargateService = ApplicationLoadBalancedFargateService.Builder.create(this, fargateServiceId)
                .cluster(cluster)
                .cpu(512)
                .desiredCount(2)
                .memoryLimitMiB(1024)
                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                        .image(ContainerImage.fromEcrRepository(ecrRepo,"latest"))
                        .executionRole(executionRole)
                        .environment(Map.of("MY_SQL_URL","jdbc:mysql://"+db.getDbInstanceEndpointAddress()+":"+db.getDbInstanceEndpointPort()+"/"+databaseName,
                                "MY_SQL_USERNAME","admin",
                                "MY_SQL_PASSWORD",databasePassword))
                        .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(logGroup)
                                .streamPrefix("ecs")
                                .build()))
                        .containerPort(80)
                        .build())
                .publicLoadBalancer(true)
                .assignPublicIp(true)
                .securityGroups(java.util.List.of(ecsSG))
                .build();


//        ApplicationListener listener = fargateService.getListener();
//
//        listener.addAction("PostUsersRule", AddApplicationActionProps.builder()
//                .priority(10) // must be unique and < 50000
//                .conditions(java.util.List.of(
//                        ListenerCondition.pathPatterns(java.util.List.of("/api/users","/api/users")),
//                        ListenerCondition.httpRequestMethods(java.util.List.of("POST","GET","PUT"))
//                ))
//                .action(ListenerAction.forward(java.util.List.of(fargateService.getTargetGroup())))
//                .build());
        // ✅ Output the DB endpoint
        CfnOutput.Builder.create(this, "DBEndpoint")
                .value(db.getDbInstanceEndpointAddress())
                .build();


    }
}
