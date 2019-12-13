import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import java.io.File;

public class S3 {


    private static AmazonS3 s3 = AmazonS3ClientBuilder.standard()
            .withCredentials(Credentials.getCredentials())
            .withRegion("us-east-1")
            .build();

    public static final   String  bucketName =  "akiax7xguadhih6xjl6n"; //Utills.uncapitalizeChars(Credentials.getCredentials().getCredentials().getAWSAccessKeyId());

    public static void createBucket() {
        System.out.println("Creating bucket " + bucketName + "\n");
        s3.createBucket(bucketName);
    }

    // upload file to bucket
    public static String uploadFile(File file) {
        System.out.println("Uploading a new file to bucket");
        String key = file.getName(); //.replace('\\', '-').replace('/', '-').replace(':', '-');
        PutObjectRequest req = new PutObjectRequest(bucketName, key, file);
        req.setCannedAcl(CannedAccessControlList.PublicRead);
        s3.putObject(req);
        return key;
    }

    //download file from bucket
    public static S3Object downloadFile(String key) {
        System.out.println("Downloading an object");
        return s3.getObject(new GetObjectRequest(bucketName, key));
    }

    // get file URL
    public static String getFileURL(String key){
        return s3.getUrl(bucketName, key).toString();
    }

    // remove file from bucket
    public static void removeFile(String key){

        s3.deleteObject(bucketName, key);
    }

    // list files in bucket (debugging)
    public static void listFiles(String bucketName){
        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                .withBucketName(bucketName));
        System.out.println("Listing files in "+bucketName+":");
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            System.out.println(" - " + objectSummary.getKey() + "  " +
                    "(size = " + objectSummary.getSize() + ")");
        }
        System.out.println();
    }
}