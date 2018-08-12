package com.movie.integration.movieintegration.ftp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileTransferHandler {

    private static final Logger logger = Logger.getLogger(FileTransferHandler.class.getName());

    @Autowired
    DefaultFtpSessionFactory ftpSessionFactory;

    @Value("${imdb.file_regex}")
    String fileRegex;

    @Value("${imdb.partial_file_regex}")
    String partialFileRegex;

    FtpSession ftpSession;

    @PostConstruct
    public void init(){
        this.ftpSession = ftpSessionFactory.getSession();
    }

    public void updateRemoteDocuments() throws IOException {

        File[] localFiles = getLocalFiles();
        HashSet<String> ftpFiles = new HashSet<>(Arrays.asList(ftpSession.listNames(".")));
        transferFiles(localFiles, ftpFiles);

    }

    private File[] getLocalFiles(){

        Pattern pattern = Pattern.compile(this.fileRegex);
        File localRootDir = new File(".");
        File[] localFiles = localRootDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return pattern.matcher(name).matches();
            }
        });

        return localFiles;

    }

    public void transferFiles(File[] localFiles, HashSet<String> ftpFiles) throws IOException {

        Pattern partialFilePattern = Pattern.compile(partialFileRegex);

        for(int i = 0; i < localFiles.length; i++){
            File file = localFiles[i];

            if(ftpFiles.contains(file.getName())){
                logger.info("ftp site already contains:" + file.getName());
                continue;
            }

            Matcher matchesExistingStaleRemoteFile = partialFilePattern.matcher(file.getName());
            Boolean foundExistingStaleRemoteFile = matchesExistingStaleRemoteFile.find();
            if(foundExistingStaleRemoteFile){
                String matchingString = matchesExistingStaleRemoteFile.group(1);
                deleteFileFromFtp(ftpFiles, matchingString);
            }
            ftpSession.write(new FileInputStream(file), file.getPath());
        }

    }

    private void deleteFileFromFtp(HashSet<String> ftpFiles, String matchingString) throws IOException {
        for(String ftpFile : ftpFiles){
            Pattern ftpPattern = Pattern.compile(".*" + matchingString + ".*");
            Boolean matches = ftpPattern.matcher(ftpFile).matches();
            logger.info("addedFile: " + matchingString + "-" + "ftpFile: " + ftpFile + " matches=" + matches);
            if(matches){
                ftpSession.getClientInstance().deleteFile(ftpFile);
                break;
            }
        }
    }

}
