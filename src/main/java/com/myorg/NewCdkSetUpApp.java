package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class NewCdkSetUpApp {
    public static void main(final String[] args) {
        App app = new App();
        StackProps props=StackProps.builder()

                .env(Environment.builder()
                        .account(System.getProperty("ACCOUNT_ID"))
                        .region("ap-south-1")
                        .build()).build();
        DataBaseandECSStack dataBaseandECSStack = new DataBaseandECSStack(app, "stack-1",props);


        new NewStackBuildStack(app, "stack-2", props, dataBaseandECSStack);
        app.synth();

    }
}

