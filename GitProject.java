import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

public class GitProject {
    public static void main(String[] args) throws IOException {
        // 2.1
        init();
        // testing 2.2
        // System.out.println(hashFile("Hello.txt"));
        // testing 2.3
        createBlob("Hello.txt");
        createBlob("test/Tester.txt");
        // testing 2.4
        addToIndex("Hello.txt");
        addToIndex("test/Tester.txt");
        addToIndex("README.md");

    }

    // method goal: Create a git/ directory if it does not already exist; create an
    // objects/
    // directory inside git/ if it does not already exist; create a file named index
    // inside git/ if
    // it does not already exist, with no extension; create a file named HEAD inside
    // git/ if it does
    // not already exist, with no extension
    public static void init() {

        File git = new File("git");
        File objects = new File("git/objects");
        File index = new File("git/index");
        File HEAD = new File("git/HEAD");
        if (objects.exists() && index.exists() && HEAD.exists())
            System.out.println("Git Repository Already Exists");

        try {
            git.mkdir();
            objects.mkdir();
            index.createNewFile();
            HEAD.createNewFile();
        } catch (IOException e) {
            System.out.println("File error: " + e.getMessage());
        }
    }

    // method goal: Write a method that takes a file path, computes the SHA-1 hash
    // of the file's
    // contents, and returns it as a String
    public static String hashFile(String filePath) throws IOException {
        Path path = Path.of(filePath);
        if (!Files.isRegularFile(path)) {
            throw new IOException("no such file: " + filePath);
        }
        byte[] fileBytes = Files.readAllBytes(path);
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
        byte[] hash = digest.digest(fileBytes);
        return HexFormat.of().formatHex(hash);
    }

    public static String createBlob(String filePath) throws IOException {
        String shaHash = hashFile(filePath);
        Path blobPath = Path.of("git/objects", shaHash);
        Files.write(blobPath, Files.readAllBytes(Path.of(filePath)));
        return shaHash;
    }

    public static void addToIndex(String filePath) throws IOException {
        String hash = createBlob(filePath);

        File index = new File("git/index");
        HashMap<String, String> indexMap = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(index));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank())
                continue;
            int space = line.indexOf(' ');
            indexMap.put(line.substring(space + 1), line.substring(0, space));
        }
        reader.close();

        if (hash.equals(indexMap.get(filePath)))
            return;

        indexMap.put(filePath, hash);

        FileWriter writer = new FileWriter(index);
        boolean isFirstEntry = true;
        for (Map.Entry<String, String> e : indexMap.entrySet()) {
            if (!isFirstEntry)
                writer.write("\n");
            writer.write(e.getValue() + " " + e.getKey());
            isFirstEntry = false;
        }
        writer.close();
    }
}