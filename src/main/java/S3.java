import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

import java.io.File;

public class S3 {

    private static AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

    private static AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(new ProfileCredentialsProvider().getCredentials());

    private static final String bucketName = "need ToGiveNameAskMoshe"; //todo: how to do with cradentials?
    // credentialsProvider.getCredentials().getAWSAccessKeyId() + "_" + directoryName
    //       .replace('\\', "").replace('/', "").replace(':', '_');

    public static void createBucket() {
        System.out.println("Creating bucket " + bucketName + "\n");
        s3.createBucket(bucketName);
    }

    // upload file to bucket
    public static String uploadFile(File file) {
        System.out.println("Uploading a new file to bucket");
        String key = file.getName().replace('\\', '-').replace('/', '-').replace(':', '-');
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
}