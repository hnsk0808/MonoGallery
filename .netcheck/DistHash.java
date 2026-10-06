import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;

/** Reproduces Gradle's PathAssembler dist-directory hash for a distribution URL. */
public class DistHash {
    public static void main(String[] args) throws Exception {
        String url = URI.create(args[0]).toString();
        byte[] digest = MessageDigest.getInstance("MD5").digest(url.getBytes());
        System.out.println(new BigInteger(1, digest).toString(36));
    }
}
