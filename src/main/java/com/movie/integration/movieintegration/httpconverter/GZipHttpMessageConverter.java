package com.movie.integration.movieintegration.httpconverter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.logging.Logger;

@Component
public class GZipHttpMessageConverter extends AbstractHttpMessageConverter<File> {

    private static final Logger logger = Logger.getLogger(GZipHttpMessageConverter.class.getName());

    public GZipHttpMessageConverter(){
        super(new MediaType("*","*"));
    }

    @Override
    protected boolean supports(Class<?> clazz){
        return File.class.isAssignableFrom(clazz);
    }

    @Override
    protected File readInternal(Class<? extends File> clazz, HttpInputMessage httpInputMessage) throws IOException {

        PushbackInputStream pushbackInputStream = (PushbackInputStream) httpInputMessage.getBody();

        logger.info("started download");

        String eTagFileName = "imdb/" + httpInputMessage.getHeaders().getETag().replace("\"", "");

        File gzippedFile = new File(eTagFileName + ".gz");
        FileOutputStream fos = new FileOutputStream(gzippedFile);

        int bytesToRead = 1_000_000;
        byte[] byteArray = new byte[bytesToRead];
        int actualBytesRead;

        while(((actualBytesRead = pushbackInputStream.read(byteArray, 0, bytesToRead)) > 0)) {
            byte[] actualBytesToWrite = new byte[actualBytesRead];
            for(int i = 0; i < actualBytesRead; i++){
                actualBytesToWrite[i] = byteArray[i];
            }
            fos.write(actualBytesToWrite);
        }

        fos.flush();
        fos.close();

        logger.info("finished file download.");

        return gzippedFile;

    }

    @Override
    protected void writeInternal(File file, HttpOutputMessage httpOutputMessage){

    }

}