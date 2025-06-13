package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.Stage;
import software.constructs.Construct;

public class MyEcsDeployStage extends Stage {
    public MyEcsDeployStage(Construct scope, String deployToFargate) {
        super(scope,deployToFargate);
       new  StackImpl(this,"pushFromPipeline");
    }
}
