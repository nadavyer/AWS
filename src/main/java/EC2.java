import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.util.Base64;

import java.util.LinkedList;
import java.util.List;

public class EC2 {

    private static AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
            .withCredentials(Credentials.getCredentials())
            .withRegion("us-east-1")
            .build();

    private static List<Instance> activeInstances = new LinkedList<Instance>();

    public static List<Instance> getActiveInstances() {
        DescribeInstancesResult result = ec2.describeInstances();
        for (int i = 0; i < result.getReservations().size(); i++) {
            List<Instance> reservationInstances = result.getReservations().get(i).getInstances();
            for (Instance inst : reservationInstances) {
                if ((inst.getState().getName().equals("running") ||
                        inst.getState().getName().equals("pending"))
                        && !activeInstances.contains(inst)) {
                    activeInstances.add(inst);
                }
            }
        }
        return activeInstances;
    }


    public static void runMachines(String key, String value){

        try {
            String imageId = "ami-00eb20669e0990cb4";
            String userData = "";
            String firstLine = "#!/bin/bash\r\n";
            if(key.equals("manager")){
                System.out.println("starting Manager EC2");
//                 //Image with java, maven, the jar file of the manager
                userData = firstLine + "java -jar /home/ec2-user/manager.jar\r\n";
            }
            else if(key.equals("worker")){
                System.out.println("starting worker EC2");
//                imageId = "ami-0fa2497f50bf0ce72"; //Image with java, maven, the updated jar file of the worker
                imageId = "ami-00eb20669e0990cb4";
                userData = firstLine + "java -jar  /home/nadav/Desktop/AWS/target/dspAss1-1.0-SNAPSHOT-jar-with-dependencies.jar\r\n";
//                firstLine + "java -cp .:yourjar.jar:stanford-corenlp-3.3.0.jar:stanford-corenlp-3.3.0-models.jar:ejml-0.23.jar:jollyday-0.4.7.jar Worker";
            }

            RunInstancesRequest request = new RunInstancesRequest(imageId, 1, 1);
            String encoded = Base64.encodeAsString(userData.getBytes());
            request.withUserData(encoded)
                    .withIamInstanceProfile(new IamInstanceProfileSpecification().withName("NE"))
                    .withSecurityGroupIds("sg-0aca71c6b04078880")
                    .setKeyName("Admin");
            request.setInstanceType(InstanceType.T2Micro.toString());

            request.setUserData(Base64.encodeAsString(userData.getBytes()));//Base64.getEncoder().encodeToString(userData.getBytes()));

            Tag t = new Tag(key, value);
            List<Tag> tags = new LinkedList<Tag>();
            tags.add(t);
            List<TagSpecification> specifications = new LinkedList<TagSpecification>();
            TagSpecification tagspec = new TagSpecification();
            tagspec.setTags(tags);
            tagspec.setResourceType("instance");

            specifications.add(tagspec);

            request.setTagSpecifications(specifications);
            ec2.runInstances(request);
//            System.out.println("**********");
//            System.out.println("runMachines - "+instances);

//            activeInstances = getActiveInstances(); //not working that way makes it double somehow
            activeInstances = ec2.describeInstances().getReservations().get(0).getInstances();

//            System.out.println("runningInstances length = "+ activeInstances.size());
//            System.out.println("runningInstances - "+activeInstances);
//            System.out.println("**********");

        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Response Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }


    private static boolean closeMachinesByTag(String tag){

        List <String> machinesIdByTag = new LinkedList<String>();

        for (Instance machine : activeInstances){
            if(machine.getTags().get(0).getKey().equals(tag)) {
                machinesIdByTag.add(machine.getInstanceId());
            }
        }

        System.out.println("running instances:");
        for (Instance inst : activeInstances){
            System.out.print("<" + inst.getTags().get(0) +", "+inst.getInstanceId()+ "> ");
        }
        System.out.println("machinesIdByTag: "+ machinesIdByTag);
        for (String id : machinesIdByTag){
            System.out.print("<" + id +"> ");
        }
        if (machinesIdByTag.size() > 0) {
            return ec2.terminateInstances(new TerminateInstancesRequest(machinesIdByTag)) != null;
        }
        return true;
    }

    public static boolean closeManager(){
        System.out.println("Trying to close manager");
        if(closeMachinesByTag("manager")){
            System.out.println("\nManager closed!");
            return true;
        }
        System.out.println("Closing manager failed!");
        return false;
    }

    public static boolean closeWorkers(){
        System.out.println("Trying to close all workers");
        if(closeMachinesByTag("worker")){
            System.out.println("All workers closed!");
            return true;
        }
        System.out.println("Closing all workers failed!");
        return false;
    }

}
