package bmr.run;

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
import org.apache.commons.io.FileUtils;

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

    private static void usage() {
        System.out.println("usage: java -classpath mipbox.jar bmr.run.CopyMR [src_dir] [dst_dir] [mr_list.txt]");
        System.out.println("\n\tstructure in directory [src_dir]: [src_dir]/[hospital]/[studyID]/[sereisID]/[dicomImage]");
        System.out.println("\teach line in [mr_list.txt]: [hospital]\t[studyID]\n");
        System.exit(0);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            usage();
        }

        File srcDir = new File(args[0]);
        if (!srcDir.exists() || !srcDir.isDirectory()) {
            usage();
        }

        File dstDir = new File(args[1]);
        if (!dstDir.exists() || !dstDir.isDirectory()) {
            usage();
        }

        File list = new File(args[2]);
        if (!list.exists() || !list.isFile()) {
            usage();
        }

        try (BufferedReader in = new BufferedReader(new FileReader(list))) {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                final String[] tokens = line.split("\t");
                if (tokens.length != 2) {
                    usage();
                }
                final String hos = tokens[0].trim();
                final String sid = tokens[1].trim();

                Path src = Paths.get(srcDir.getPath() + "/" + hos + "/" + sid);
                Path dst = Paths.get(dstDir.getPath() + "/" + hos + "/" + sid);
                if (src.toFile().exists() && !dst.toFile().exists()) {
                    System.out.println(src + " -> " + dst);
                    FileUtils.copyDirectory(src.toFile(), dst.toFile());
                }

            }
        }

    }

    private static void compareAndCopy() throws IOException {
        final String SOURCE_ROOT = "/media/ju/SONY HD-E2/_BREAST_MRI/DICOM/";
        final String TARGET_ROOT = "/home/ju/workspace/_BREAST_MRI/";
        HashSet<Path> mr = new HashSet<>();
        try (BufferedReader in = new BufferedReader(new FileReader(TARGET_ROOT + "MR.txt"))) {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                String[] tokens = line.split("\t");
                assert (tokens.length == 2);
                String hospital = tokens[0].trim();
                String studyID = tokens[1].trim();

                Path p = Paths.get(SOURCE_ROOT + hospital + "/" + studyID);
                mr.add(p);

            }
        }

        HashSet<Path> mr_new = new HashSet<>();
        try (BufferedReader in = new BufferedReader(new FileReader(TARGET_ROOT + "MR_new.txt"))) {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                String[] tokens = line.split("\t");
                assert (tokens.length == 2);
                String hospital = tokens[0].trim();
                String studyID = tokens[1].trim();

                Path p = Paths.get(SOURCE_ROOT + hospital + "/" + studyID);
                mr_new.add(p);

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
                final Path hos = p_new.getName(p_new.getNameCount() - 2);
                final Path sid = p_new.getName(p_new.getNameCount() - 1);
                File target = new File(
                        TARGET_ROOT + hos + "/" + sid
                );
                if (!target.exists()) {
                    System.out.println(p_new);
                }
                //FileUtils.copyDirectory(p_new.toFile(), target);
            }
        }
    }

    private static long size(Path path) {

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
