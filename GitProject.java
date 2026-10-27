import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;



public class GitProject {
    public static void main(String[] args) throws IOException {
        init();

        addToIndex("Hello.txt");
        addToIndex("Tester.txt");
        addToIndex("test/Tester.txt");
        addToIndex("README.md");

        addToIndex("file.txt");
        addToIndex("test/file.txt");

        ArrayList<String> workingList = createWorkingList();
        System.out.println("Working List:");
        for (String e : workingList) {
            System.out.println(e);
        }

        String first = createTree(workingList, "test");
        String second = createTree(workingList, "test");
        System.out.println("\ntest tree hash: " + first);
        System.out.println("same hash both times: " + first.equals(second));
        System.out.println("contents:\n" + Files.readString(Path.of("git/objects/" + first)));

        String root = buildTree(workingList);
        System.out.println("\nroot tree hash: " + root);
        System.out.println("contents:\n" + Files.readString(Path.of("git/objects/" + root)));
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

    // i feel as if this is doing too much, but it is what it is
    public static void addToIndex(String filePath) throws IOException {
        String hash = createBlob(filePath);

        File index = new File("git/index");
        HashMap<String, String> indexMap = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(index));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank())
                continue;
            // int space = line.indexOf(' '); // (working during 3.2) I just realized that
            // sha-1 hash is always 40 characters.
            // indexMap.put(line.substring(space + 1), line.substring(0, space));
            // path | hash
            indexMap.put(line.substring(41), line.substring(0, 40));
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

    public static String createTree(ArrayList<String> workingList, String dirPath) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        // ^^ lines is comprised of the children

        for (String e : workingList) {
            // the format is blob + hash + path. blob is 4 characters, and hash is always
            // 40, so that makes my life so much easier
            String type = e.substring(0, 4);
            String hash = e.substring(5, 45);
            String path = e.substring(46);

            String dir = parentDir(path);
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (dir.equals(dirPath))
                lines.add(type + " " + hash + " " + name);

        }
        lines.sort((a, b) -> a.substring(46).compareTo(b.substring(46))); // https://www.w3schools.com/java/ref_arraylist_sort.asp
        String content = String.join("\n", lines);
        String treeHash = hashString(content);
        FileWriter writer;
        try {
            writer = new FileWriter(new File("git/objects/" + treeHash));
            writer.write(content);
            writer.close();
        } catch (IOException exception) {
            throw new IOException("Error writing tree object to file: " + exception.getMessage());
        }
        return treeHash;
    }

    public static String buildTree(ArrayList<String> workingList) throws IOException {
        String theBiggestTheLargest = ""; // this is the deepest directory. I was listening to BigXthaPlug
        for (String e : workingList) {
            String dir = parentDir(e.substring(46));
            if (dir.length() > theBiggestTheLargest.length())
                theBiggestTheLargest = dir;
        }

        // this is the base case. if there are no directories left, we can just create a tree and return its hash; this is the root
        if (theBiggestTheLargest.isEmpty()) {
            return createTree(workingList, "");
        }
            

        String treeHash = createTree(workingList, theBiggestTheLargest);

        // drop the directory's children and replace them with one tree entry
        ArrayList<String> newWorkingList = new ArrayList<>();
        for (String e : workingList) {
            if (!parentDir(e.substring(46)).equals(theBiggestTheLargest))
                newWorkingList.add(e);
            // seems like this effectively collapses all of this directories children out of the new working list
        }
        newWorkingList.add("tree " + treeHash + " " + theBiggestTheLargest);
        return buildTree(newWorkingList); // we don't gotta re-sort because create tree will
    }

    public static String hashString(String content) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    public static String parentDir(String path) {
        if (path.lastIndexOf('/') == -1)
            return "";
        return path.substring(0, path.lastIndexOf('/'));
    }

    public static ArrayList<String> createWorkingList() {
        ArrayList<String> workingList = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File("git/index")));
            String line;
            while ((line = reader.readLine()) != null) {
                workingList.add("blob " + line);
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("File error: " + e.getMessage());
        }
        workingList.sort((a, b) -> a.substring(46).compareTo(b.substring(46))); // https://www.w3schools.com/java/ref_arraylist_sort.asp
        return workingList;
    }
}