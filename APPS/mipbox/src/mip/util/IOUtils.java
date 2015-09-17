package mip.util;

import java.io.File;
import java.io.IOException;
import static java.nio.file.FileVisitResult.CONTINUE;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

public class IOUtils {

    private IOUtils() { // singleton
    }

    public static ArrayList<Path> listFiles(String dir) {
        return new FileVisitor(dir).getFiles();
    }

    public static boolean fileExisted(String filepath) {
        try {
            File f = new File(filepath);
            return f.isFile() && f.exists();
        } catch (Throwable ignore) {
            return false;
        }
    }
}

class FileVisitor {

    private final String srcRoot;
    ArrayList<Path> files;

    public FileVisitor(String root) {
        srcRoot = root;
        files = new ArrayList<>();
    }

    private void walkFiles() {
        try {
            Files.walkFileTree(Paths.get(srcRoot), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName() != null) {
                        files.add(file);
                    }
                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    //System.err.printf("Visiting failed for %s\n", file);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    //System.out.printf("About to visit directory %s\n", dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Throwable ignore) {
        }
    }

    public ArrayList<Path> getFiles() {
        walkFiles();
        return files;
    }
}
