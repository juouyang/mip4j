
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author ju
 */
public class CopyMR {

    private static final String SOURCE_ROOT = "/media/ju/SONY HD-E2/_BREAST_MRI/DICOM/";
    private static final String TARGET_ROOT = "/home/ju/workspace/_BREAST_MRI/";

    public static void main(String[] args) throws IOException {
        HashSet<Path> mr = new HashSet<>();
        try (BufferedReader in = new BufferedReader(new FileReader(TARGET_ROOT + "MR.txt"))) {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                //<editor-fold defaultstate="collapsed" desc="Process Each MR Study">
                String[] tokens = line.split("\t");
                assert (tokens.length == 2);
                String hospital = tokens[0].trim();
                String studyID = tokens[1].trim();

                Path p = Paths.get(SOURCE_ROOT + hospital + "/" + studyID);
                mr.add(p);
                //</editor-fold>
            }
        }

        HashSet<Path> mr_new = new HashSet<>();
        try (BufferedReader in = new BufferedReader(new FileReader(TARGET_ROOT + "MR_new.txt"))) {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                //<editor-fold defaultstate="collapsed" desc="Process Each MR Study">
                String[] tokens = line.split("\t");
                assert (tokens.length == 2);
                String hospital = tokens[0].trim();
                String studyID = tokens[1].trim();

                Path p = Paths.get(SOURCE_ROOT + hospital + "/" + studyID);
                mr_new.add(p);
                //</editor-fold>
            }
        }

        for (Path p : mr) {
            boolean duplicate = false;
            for (Path p_new : mr_new) {
                if (p.equals(p_new)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                System.out.println("\t" + p);
            }
        }

        for (Path p_new : mr_new) {
            boolean existed = false;
            for (Path p : mr) {
                if (p_new.equals(p)) {
                    existed = true;
                    break;
                }
            }
            if (!existed) {
                File target = new File(
                        TARGET_ROOT + p_new.getName(p_new.getNameCount() - 2) + "/" + p_new.getName(p_new.getNameCount() - 1)
                );
                if (!target.exists()) {
                    System.out.println(p_new);
                }
                //FileUtils.copyDirectory(p_new.toFile(), target);
            }
        }
    }

    public static long
            size(Path path) {

        final AtomicLong size = new AtomicLong(0);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult
                        visitFile(Path file, BasicFileAttributes attrs) {

                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult
                        visitFileFailed(Path file, IOException exc) {

                    System.out.println("skipped: " + file + " (" + exc + ")");
                    // Skip folders that can't be traversed
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult
                        postVisitDirectory(Path dir, IOException exc) {

                    if (exc != null) {
                        System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                    }
                    // Ignore errors traversing a folder
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }

        return size.get();
    }
}
