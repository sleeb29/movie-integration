package com.movie.integration.movieintegration.local;

import com.movie.integration.movieintegration.util.FileHandlerUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class LocalFileHandler {

    @Value("${imdb.file_regex}")
    String fileRegex;

    @Value("${imdb.local_directory")
    String localDirectory;

    @Value("${imdb.partial_file_regex}")
    String partialFileRegex;

    String repositoryFile = "tag_repository/tag_file";

    public void cleanUpLocalFiles() throws IOException {

        HashSet<File> newFiles = FileHandlerUtils.getLocalFiles(this.fileRegex);
        Set<String> currentFiles = null;

        File oldFile = new File(repositoryFile);

        Boolean foundDifferences = true;
        Pattern partialFilePattern = Pattern.compile(partialFileRegex);

        if(oldFile.exists()) {
            Set<String> oldFiles = getOldFiles(oldFile);
            foundDifferences = differenceBetweenRuns(newFiles, oldFiles);
            if(foundDifferences){
                archiveOldRepositoryFile(oldFile);
                currentFiles = FileHandlerUtils.mergeOldAndNewFiles(partialFilePattern, newFiles, oldFiles);
            }
        } else {
            currentFiles = FileHandlerUtils.mergeOldAndNewFiles(partialFilePattern, newFiles, new HashSet<>());
        }

        if(foundDifferences) {
            createNewRepositoryFile(currentFiles);
        }

        FileHandlerUtils.deleteFilesInDirectoryNotMatchingPattern("imdb", fileRegex);

    }

    private Set<String> getOldFiles(File oldRepositoryFile) throws IOException {


        BufferedReader br = new BufferedReader(new FileReader(oldRepositoryFile));

        Set<String> oldFiles = new HashSet<>();
        String oldFile;
        while ((oldFile = br.readLine()) != null) {
            oldFiles.add(oldFile);
        }

        return oldFiles;

    }

    private Boolean differenceBetweenRuns(HashSet<File> newFiles, Set<String> oldFiles){

        for(File file : newFiles){

            String fileName = file.getName();

            if(!oldFiles.contains(fileName)){
                return true;
            }

        }

        return false;

    }

    private void archiveOldRepositoryFile(File oldFile){

        File archiveFile = new File("tag_repository/archive/");
        if(!archiveFile.exists()){
            archiveFile.mkdir();
        }

        Date date = new Date(oldFile.lastModified());
        SimpleDateFormat jdf = new SimpleDateFormat("yyyyMMddHHmmssZ");
        jdf.setTimeZone(TimeZone.getTimeZone("America/Detroit"));
        String fileDateString = jdf.format(date);
        oldFile.renameTo(new File("tag_repository/archive/tag_file_" + fileDateString));

    }

    private void createNewRepositoryFile(Set<String> currentFiles) throws IOException {

        File newFile = new File(repositoryFile);
        newFile.createNewFile();

        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(newFile);

            Iterator<String> fileIterator = currentFiles.iterator();
            int i = 0;
            while(fileIterator.hasNext()){
                String fileName = fileIterator.next();
                fileWriter.append(fileName);
                if (i < currentFiles.size() - 1) {
                    fileWriter.append("\n");
                }
                i++;
            }

            fileWriter.flush();

        } finally {
            if(fileWriter != null){
                fileWriter.close();
            }
        }

    }

}
