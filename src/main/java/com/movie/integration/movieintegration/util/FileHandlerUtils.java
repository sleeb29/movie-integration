package com.movie.integration.movieintegration.util;

import com.movie.integration.movieintegration.ftp.FileTransferHandler;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileHandlerUtils {

    private static final Logger logger = Logger.getLogger(FileHandlerUtils.class.getName());

    public static HashSet<File> getLocalFiles(String fileRegex){

        Pattern pattern = Pattern.compile(fileRegex);
        File localRootDir = new File("imdb");
        File[] localFiles = localRootDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return pattern.matcher(name).matches();
            }
        });

        return new HashSet<>(Arrays.asList(localFiles));

    }

    public static void deleteFilesInDirectoryNotMatchingPattern(String filePath, String patternToIgnore){

        File directoryToRemoveFilesFrom = new File(filePath);
        Pattern pattern = Pattern.compile(patternToIgnore);
        File[] filesToRemove = directoryToRemoveFilesFrom.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !pattern.matcher(name).matches();
            }
        });

        for(File file : filesToRemove){
            file.delete();
        }

    }

    public static Set<String> mergeOldAndNewFiles(Pattern partialFilePattern, Set<File> newFiles, Set<String> oldFiles){

        Set<String> currentFiles = newFiles.parallelStream().map(newFile -> newFile.getName()).collect(Collectors.toSet());
        Iterator<File> fileIterator = newFiles.iterator();

        oldFiles.parallelStream().forEach(oldFile -> {

            String matchingString = getMatchingString(partialFilePattern, oldFile);
            if(matchingString == null){
                return;
            }

            Pattern filePattern = Pattern.compile(".*" + matchingString + ".*");

            while(fileIterator.hasNext()){
                Boolean matches = filePattern.matcher(fileIterator.next().getName()).matches();
                logger.info("addedFile: " + matchingString + "-" + "oldFile: " + oldFile + " matches=" + matches);
                if(matches){
                    return;
                }
            }

            currentFiles.add(oldFile);

        });

        return currentFiles;

    }

    public static String getMatchingString(Pattern partialFilePattern, String fileName){

        Matcher matchesExistingStaleRemoteFile = partialFilePattern.matcher(fileName);
        Boolean foundExistingStaleRemoteFile = matchesExistingStaleRemoteFile.find();

        String matchingString = null;

        if(foundExistingStaleRemoteFile){
            Boolean foundSecondCaptureGroup = matchesExistingStaleRemoteFile.find();
            if(foundSecondCaptureGroup){
                matchingString = matchesExistingStaleRemoteFile.group(1);
            }
        }

        return matchingString;

    }

}
