package com.movie.integration.movieintegration.ftp;

import com.movie.integration.movieintegration.util.FileHandlerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Logger;
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

        HashSet<File> localFiles = FileHandlerUtils.getLocalFiles(this.fileRegex);
        String[] remoteFiles = ftpSession.listNames("imdb");
        HashSet<String> ftpFiles = new HashSet<>();
        if(remoteFiles != null && remoteFiles.length > 0){
            ftpFiles = new HashSet<>(Arrays.asList(remoteFiles));
        }
        transferFiles(localFiles, ftpFiles);

    }

    public void transferFiles(HashSet<File> localFiles, HashSet<String> ftpFiles) {

        Pattern partialFilePattern = Pattern.compile(partialFileRegex);

        localFiles.parallelStream().forEach(localFile -> {
            if(ftpFiles.contains(localFile.getName())){
                logger.info("ftp site already contains:" + localFile.getName());
                return;
            }

            String matchingString = FileHandlerUtils.getMatchingString(partialFilePattern, localFile.getName());

            if(matchingString != null){
                try {
                    deleteFileFromFtp(ftpFiles, matchingString);
                } catch(IOException e){
                    e.printStackTrace();
                }
            }

            try {
                ftpSession.write(new FileInputStream(localFile), localFile.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

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
